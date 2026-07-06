package com.faendir.kotlin.autodsl

import com.faendir.kotlin.autodsl.parameter.Parameter
import com.faendir.kotlin.autodsl.parameter.ParameterFactory
import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode
import io.github.enjoydambience.kotlinbard.TypeSpecBuilder
import io.github.enjoydambience.kotlinbard.addAnnotation
import io.github.enjoydambience.kotlinbard.addClass
import io.github.enjoydambience.kotlinbard.addCode
import io.github.enjoydambience.kotlinbard.addFunction
import io.github.enjoydambience.kotlinbard.addInterface
import io.github.enjoydambience.kotlinbard.addObject
import io.github.enjoydambience.kotlinbard.addProperty
import io.github.enjoydambience.kotlinbard.buildFile
import io.github.enjoydambience.kotlinbard.codeFmt
import io.github.enjoydambience.kotlinbard.controlFlow
import io.github.enjoydambience.kotlinbard.`if`
import io.github.enjoydambience.kotlinbard.nullable
import java.util.Locale
import kotlin.jvm.internal.DefaultConstructorMarker
import kotlin.properties.Delegates

const val DEFAULTS_BITFLAGS_FIELD_NAME = "_defaultsBitField"

class DslGenerator<A, T : A, C : A, P : A>(
    private val kotlinVersion: KotlinVersion,
    private val logger: Logger<T>,
    private val codeGenerator: CodeWriter<T>,
    resolver: SourceInfoResolver<A, T, C, P>,
) : SourceInfoResolver<A, T, C, P> by resolver {
    private val parameterFactory = ParameterFactory(resolver)

    private fun error(
        reason: String,
        clazz: T,
    ) {
        logger.error("@AutoDsl can't be applied to $clazz: $reason", clazz)
    }

    fun process(): List<T> =
        getClassesWithAnnotation(AutoDsl::class)
            .flatMap { processClass(it, it.getAnnotationTypeProperty(AutoDsl::class, AutoDsl::dslMarker)) }

    private fun processClass(
        clazz: T,
        markerType: ClassName?,
    ): List<T> {
        if (clazz.isSealed()) {
            val subclasses = clazz.getSealedSubclasses()
            val deferredChildren = subclasses.flatMap { processClass(it, markerType) }

            generateSealedOrchestrator(clazz, subclasses, markerType)

            if (deferredChildren.isNotEmpty()) {
                return deferredChildren + clazz
            }
            return emptyList()
        }
        when (clazz.getClassKind()) {
            ClassKind.INTERFACE -> {
                error("must not be an interface", clazz)
            }

            ClassKind.ENUM_CLASS -> {
                error("must not be an enum class", clazz)
            }

            ClassKind.ENUM_ENTRY -> {
                error("must not be an enum entry", clazz)
            }

            ClassKind.OBJECT -> {
                error("must not be an object", clazz)
            }

            ClassKind.ANNOTATION_CLASS -> {
                val deferred = getClassesWithAnnotation(clazz).flatMap { processClass(it, markerType) }
                if (deferred.isNotEmpty()) {
                    return deferred + clazz
                }
            }

            ClassKind.CLASS -> {
                if (!generate(clazz, markerType)) {
                    return listOf(clazz)
                }
            }
        }
        return emptyList()
    }

    /**
     * returns true if class generation was successful
     */
    private fun generate(
        entity: T,
        markerType: ClassName?,
    ): Boolean {
        if (entity.isAbstract() && !entity.isSealed()) {
            error("must not be abstract", entity)
            return false
        }
        val constructors = entity.getConstructors().filter { it.isAccessible() }
        if (constructors.isEmpty()) {
            error("must have at least one public or internal constructor", entity)
            return false
        }
        val constructor =
            constructors.firstOrNull { it.hasAnnotation(AutoDslConstructor::class) }
                ?: entity.getPrimaryConstructor()
                ?: constructors.first()
        if (!constructor.isValid()) {
            // defer processing
            return false
        }
        val parameters = parameterFactory.getParameters(constructor, entity)
        val entityClass = entity.asClassName()
        val builderClass = entityClass.withBuilderSuffix()
        val entityTypeParameters = entity.getTypeParameters()
        val entityType: TypeName = entityClass.parameterize(entityTypeParameters)
        val builderType: TypeName = builderClass.parameterize(entityTypeParameters)
        val bitFieldIndices = 0..parameters.size / 32
        val typePrefix = entityClass.simpleNames.joinToString("")
        buildFile(entityClass.packageName, "${typePrefix}Dsl") {
            addClass(builderClass.simpleName) {
                for (typeParam in entityTypeParameters) {
                    addTypeVariable(typeParam.withoutVariance() as TypeVariableName)
                }
                if (parameters.any { it.hasDefault }) {
                    for (i in bitFieldIndices) {
                        addProperty(DEFAULTS_BITFLAGS_FIELD_NAME + i, INT, KModifier.PRIVATE) {
                            mutable(true)
                            initializer("-1")
                        }
                    }
                }
                val operatorCollisions = mutableSetOf<TypeName>()
                val infixCollisions = mutableSetOf<String>()
                for (parameter in parameters) {
                    addParameterProperty(parameter, entityClass)
                    addParameterBuilderStyleSetter(parameter, builderType, entityClass)
                    if (parameter.collectionType != null) {
                        addParameterBuilderStyleVarargSetter(parameter, builderType, entityClass)
                        if (parameter.hasNestedDsl) {
                            val rawType = (parameter.typeName as? ParameterizedTypeName)?.typeArguments?.first() ?: parameter.typeName
                            val parameterClazz =
                                getClassesWithAnnotation(AutoDsl::class).firstOrNull {
                                    it.asClassName().canonicalName ==
                                        rawType.toRawType().canonicalName
                                }
                            if (parameterClazz != null && parameterClazz.isSealed()) {
                                addFlavoredInfixBlocks(parameter, parameterClazz, builderClass, infixCollisions)
                            } else {
                                addParameterNestedAdder(parameter)
                            }
                        }
                        if (operatorCollisions.add(parameter.typeName)) {
                            addParameterOperatorAdder(parameter)
                        }
                    } else if (parameter.hasNestedDsl) {
                        val parameterClazz =
                            getClassesWithAnnotation(AutoDsl::class).firstOrNull {
                                it.asClassName().canonicalName ==
                                    parameter.typeName.toRawType().canonicalName
                            }
                        if (parameterClazz != null && parameterClazz.isSealed()) {
                            addFlavoredInfixBlocks(parameter, parameterClazz, builderClass, infixCollisions)
                        }
                        addParameterNestedSetter(parameter)
                    }
                }
                addFunction("build") {
                    returns(entityType)
                    parameters.filter { it.isMandatory }.groupBy { it.group }.forEach { (_, parameters) ->
                        addStatement(
                            "check(%L)·{ \"%L·must·be·assigned.\" }",
                            parameters.map { "%L != null".codeFmt(it.name) }.joinToCode(" || "),
                            parameters.joinToString(separator = ",·", prefix = if (parameters.size > 1) "One·of·" else "") { it.name },
                        )
                    }
                    if (parameters.any { it.hasDefault }) {
                        addStatement(
                            "return %T::class.java.getConstructor(%L, %L, %L).newInstance(%L, %L, null) as %T",
                            entityClass,
                            parameters
                                .map {
                                    "%T::class.%L".codeFmt(
                                        it.typeName.toRawType().nonnull,
                                        if (it.typeName.isNullable) "javaObjectType" else "java",
                                    )
                                }.joinToCode(),
                            bitFieldIndices.map { "%T::class.java".codeFmt(INT) }.joinToCode(),
                            if (kotlinVersion.isAtLeast(1, 5)) {
                                "%T::class.java".codeFmt(DefaultConstructorMarker::class.asClassName())
                            } else {
                                "Class.forName(%S)".codeFmt(DefaultConstructorMarker::class.java.name)
                            },
                            parameters
                                .map {
                                    when {
                                        it.typeName.isNullable -> "%L"
                                        it.typeName == BOOLEAN -> "%L ?: false"
                                        it.typeName == BYTE -> "%L ?: 0"
                                        it.typeName == SHORT -> "%L ?: 0"
                                        it.typeName == INT -> "%L ?: 0"
                                        it.typeName == LONG -> "%L ?: 0"
                                        it.typeName == CHAR -> "%L ?: '\\u0000'"
                                        it.typeName == FLOAT -> "%L ?: 0.0f"
                                        it.typeName == DOUBLE -> "%L ?: 0.0"
                                        else -> "%L"
                                    }.codeFmt(it.name)
                                }.joinToCode(),
                            bitFieldIndices.map { "%L".codeFmt(DEFAULTS_BITFLAGS_FIELD_NAME + it) }.joinToCode(),
                            entityType,
                        )
                    } else {
                        addStatement(
                            "return %T(%L)",
                            entityClass,
                            parameters.map { (if (it.typeName.isNullable) "%L" else "%L!!").codeFmt(it.name) }.joinToCode(),
                        )
                    }
                }
                addAnnotation(DslInspect::class)
                if (markerType != null && markerType != Annotation::class.asClassName()) {
                    addAnnotation(markerType)
                }
            }
            addFunction(typePrefix.replaceFirstChar { it.lowercase(Locale.getDefault()) }) {
                addModifiers(KModifier.INLINE)
                addAnnotation(ClassName("kotlin", "OptIn")) {
                    addMember("%T::class", ClassName("kotlin.contracts", "ExperimentalContracts"))
                }
                for (typeParam in entityTypeParameters) {
                    addTypeVariable(typeParam.withoutVariance() as TypeVariableName)
                }
                addParameter("initializer", builderType.asLambdaReceiver())
                addCode {
                    addStatement("return %T().apply(initializer).build()", builderType)
                }
                returns(entityType)
            }
        }.writeTo(entity, codeGenerator)
        return true
    }

    private fun ClassName.parameterize(typeParameters: List<TypeVariableName>) =
        if (typeParameters.isEmpty()) this else parameterizedBy(typeParameters)

    private fun TypeSpecBuilder.addParameterNestedSetter(parameter: Parameter) =
        addFunction(parameter.name) {
            addModifiers(KModifier.INLINE)
            addAnnotation(ClassName("kotlin", "OptIn")) {
                addMember("%T::class", ClassName("kotlin.contracts", "ExperimentalContracts"))
            }
            val nestedBuilderType =
                if (parameter.typeName is ParameterizedTypeName) {
                    val classVars = parameter.typeParameters!!
                    val typeArgs =
                        parameter.typeName.typeArguments.mapIndexed { index, arg ->
                            if (arg is WildcardTypeName) {
                                if (arg.outTypes == STAR.outTypes) {
                                    val type =
                                        classVars[index].let {
                                            TypeVariableName(
                                                it.name + index,
                                                it.bounds,
                                                it.variance,
                                            )
                                        }
                                    addTypeVariable(type)
                                    type
                                } else {
                                    val type =
                                        classVars[index].let {
                                            TypeVariableName(
                                                it.name + index,
                                                it.bounds + arg.outTypes,
                                                it.variance,
                                            )
                                        }
                                    addTypeVariable(type)
                                    type
                                }
                            } else {
                                arg
                            }
                        }
                    parameter.typeName.rawType
                        .parameterizedBy(typeArgs)
                        .withBuilderSuffix()
                } else {
                    parameter.typeName.withBuilderSuffix()
                }
            addParameter("initializer", nestedBuilderType.asLambdaReceiver())
            addCode {
                addStatement("kotlin.contracts.contract { callsInPlace(initializer, kotlin.contracts.InvocationKind.EXACTLY_ONCE) }")
                addStatement("val result = %T().apply(initializer).build()", nestedBuilderType)
                addStatement("%L = result", parameter.name)
                addStatement("return result")
            }
            returns(parameter.typeName.nonnull)
        }

    private fun TypeSpecBuilder.addParameterNestedAdder(parameter: Parameter) =
        addFunction(parameter.collectionType!!.singular) {
            addModifiers(KModifier.INLINE)
            addAnnotation(ClassName("kotlin", "OptIn")) {
                addMember("%T::class", ClassName("kotlin.contracts", "ExperimentalContracts"))
            }
            val rawElementType = (parameter.typeName as ParameterizedTypeName).typeArguments.first()
            val elementType =
                if (rawElementType is ParameterizedTypeName) {
                    val classVars = parameter.typeParameters!!
                    val typeArgs =
                        rawElementType.typeArguments.mapIndexed { index, arg ->
                            if (arg is WildcardTypeName) {
                                if (arg.outTypes == STAR.outTypes) {
                                    val type = classVars[index].let { TypeVariableName(it.name + index, it.bounds, it.variance) }
                                    addTypeVariable(type)
                                    type
                                } else {
                                    val type = classVars[index].let { TypeVariableName(it.name + index, arg.outTypes, it.variance) }
                                    addTypeVariable(type)
                                    type
                                }
                            } else {
                                arg
                            }
                        }
                    rawElementType.rawType.parameterizedBy(typeArgs)
                } else {
                    rawElementType
                }
            val nestedBuilderType = elementType.withBuilderSuffix()
            addParameter("initializer", nestedBuilderType.asLambdaReceiver())
            addCode {
                addStatement("kotlin.contracts.contract { callsInPlace(initializer, kotlin.contracts.InvocationKind.EXACTLY_ONCE) }")
                addStatement("val result = %T().apply(initializer).build()", nestedBuilderType)
                addStatement(
                    "%1L = %1L?.plus(result) ?: %2L(result)",
                    parameter.name,
                    parameter.collectionType.createFunction,
                )
                addStatement("return result")
            }
            returns(elementType.nonnull)
        }

    private fun TypeSpecBuilder.addParameterBuilderStyleVarargSetter(
        parameter: Parameter,
        builderType: TypeName,
        entityClass: ClassName,
    ) = addFunction("with${parameter.name.replaceFirstChar { it.uppercase() }}") {
        val elementType =
            (parameter.typeName as ParameterizedTypeName).typeArguments.first().let {
                if (it is WildcardTypeName) it.outTypes.first() else it
            }
        returns(builderType)
        addParameter(parameter.name, elementType, KModifier.VARARG)
        addStatement("this.%1L·= %1L.%2L()", parameter.name, parameter.collectionType!!.convertFunction)
        addStatement("return this")
        parameter.doc?.let { addKdoc(it) }
        addKdoc("@see %T.%L", entityClass, parameter.name)
    }

    private fun TypeSpecBuilder.addParameterBuilderStyleSetter(
        parameter: Parameter,
        builderType: TypeName,
        entityClass: ClassName,
    ) = addFunction("with${parameter.name.replaceFirstChar { it.uppercase() }}") {
        returns(builderType)
        addParameter(parameter.name, parameter.typeName)
        addStatement("this.%1L·= %1L", parameter.name)
        addStatement("return this")
        parameter.doc?.let { addKdoc(it) }
        addKdoc("@see %T.%L", entityClass, parameter.name)
    }

    private fun TypeSpecBuilder.addParameterProperty(
        parameter: Parameter,
        entityClass: ClassName,
    ) = addProperty(parameter.name, parameter.typeName.nullable) {
        mutable(true)
        if (parameter.hasDefault) {
            delegate(
                "%1T.observable(null)·{·_, _, _·-> %2L·= %2L and %3L }",
                Delegates::class.asClassName(),
                DEFAULTS_BITFLAGS_FIELD_NAME + (parameter.index / 32),
                (1 shl parameter.index % 32).inv(),
            )
        } else {
            initializer("null")
        }
        if (parameter.isMandatory) {
            addAnnotation(DslMandatory::class) {
                useSiteTarget(AnnotationSpec.UseSiteTarget.SET)
                addMember("group = %S", parameter.group)
            }
        }
        parameter.doc?.let { addKdoc(it) }
        addKdoc("@see %T.%L", entityClass, parameter.name)
    }

    private fun TypeSpecBuilder.addParameterOperatorAdder(parameter: Parameter) {
        val safeType =
            parameter.typeName
                .let { type ->
                    if (type is ParameterizedTypeName) {
                        val safeArgs =
                            type.typeArguments.map {
                                if (it == STAR) Any::class.asClassName().nullable else it
                            }
                        type.rawType.parameterizedBy(safeArgs)
                    } else {
                        type
                    }
                }.withoutVariance()
        val elementType = (safeType as ParameterizedTypeName).typeArguments.first()
        addFunction("plus") {
            receiver(safeType.nullable)
            returns(safeType)
            addAnnotation(JvmName::class) {
                addMember("%S", "add${parameter.collectionType!!.singular.replaceFirstChar { it.uppercase() }}")
            }
            addModifiers(KModifier.OPERATOR)
            addParameter("value", elementType)
            addCode {
                controlFlow(
                    "return %L",
                    parameter.collectionType!!.builderFunction,
                ) {
                    `if`("this@plus != null") {
                        addStatement("addAll(this@plus)")
                    }
                    addStatement("add(value)")
                }
            }
        }
        addFunction("plus") {
            receiver(safeType.nullable)
            returns(safeType)
            addAnnotation(JvmName::class) {
                addMember("%S", "addAll${parameter.collectionType!!.singular.replaceFirstChar { it.uppercase() }}")
            }
            addModifiers(KModifier.OPERATOR)
            addParameter("value", safeType)
            addCode {
                controlFlow(
                    "return %L",
                    parameter.collectionType!!.builderFunction,
                ) {
                    `if`("this@plus != null") {
                        addStatement("addAll(this@plus)")
                    }
                    addStatement("addAll(value)")
                }
            }
        }
    }

    private fun generateSealedOrchestrator(
        parentClazz: T,
        subclasses: List<T>,
        markerType: ClassName?,
    ) {
        val parentType = parentClazz.asClassName()
        val parentName = parentType.simpleName
        val parentBuilderType = parentType.withBuilderSuffix()
        val typePrefix = parentType.simpleNames.joinToString("")
        val typeVariables = parentClazz.getTypeParameters()
        val parameterizedParentType = if (typeVariables.isNotEmpty()) parentType.parameterizedBy(typeVariables) else parentType
        val parameterizedParentBuilderType =
            if (typeVariables.isNotEmpty()) {
                parentBuilderType.parameterizedBy(
                    typeVariables,
                )
            } else {
                parentBuilderType
            }

        buildFile(parentType.packageName, "${typePrefix}Dsl") {
            addClass(parentBuilderType.simpleName) {
                if (typeVariables.isNotEmpty()) {
                    addTypeVariables(typeVariables)
                }

                addProperty("_result", parameterizedParentType.nullable, KModifier.INTERNAL) {
                    mutable(true)
                    initializer("null")
                    addAnnotation(PublishedApi::class)
                }

                for (subclass in subclasses) {
                    val subclassName = subclass.asClassName()
                    val typeVariableNames = subclass.getTypeParameters()
                    val subclassBuilderType = subclassName.withBuilderSuffix()
                    val parameterizedSubclassBuilderType =
                        if (typeVariableNames.isNotEmpty()) {
                            subclassBuilderType.parameterizedBy(typeVariableNames)
                        } else {
                            subclassBuilderType
                        }

                    val parameterizedSubclassName =
                        if (typeVariableNames.isNotEmpty()) {
                            subclassName.parameterizedBy(typeVariableNames)
                        } else {
                            subclassName
                        }
                    val subclassSimpleName = subclassName.simpleName

                    val cleanedName =
                        if (subclassSimpleName.endsWith(parentName, ignoreCase = true) &&
                            subclassSimpleName.length > parentName.length
                        ) {
                            subclassSimpleName.removeSuffix(parentName)
                        } else {
                            subclassSimpleName
                        }

                    val subclassBuilderName = cleanedName.replaceFirstChar { it.lowercase(Locale.getDefault()) }

                    addFunction(subclassBuilderName) {
                        if (typeVariableNames.isNotEmpty()) {
                            addTypeVariables(typeVariableNames)
                        }
                        addAnnotation(Suppress::class) {
                            addMember("%S", "UNCHECKED_CAST")
                        }
                        addAnnotation(ClassName("kotlin", "OptIn")) {
                            addMember("%T::class", ClassName("kotlin.contracts", "ExperimentalContracts"))
                        }
                        addModifiers(KModifier.INLINE)

                        addParameter("initializer", parameterizedSubclassBuilderType.asLambdaReceiver())
                        addCode {
                            addStatement(
                                "kotlin.contracts.contract { callsInPlace(initializer, kotlin.contracts.InvocationKind.EXACTLY_ONCE) }",
                            )
                            addStatement("val built = %T().apply(initializer).build()", parameterizedSubclassBuilderType)
                            addStatement("_result = built as %T", parameterizedParentType)
                            addStatement("return@$subclassBuilderName built")
                        }
                        returns(parameterizedSubclassName)
                    }
                }

                addFunction("build") {
                    returns(parameterizedParentType)
                    addStatement(
                        "return _result ?: throw %T(%S)",
                        IllegalArgumentException::class.asClassName(),
                        "No sealed subclass variant was configured for ${parentType.simpleName}",
                    )
                }

                addAnnotation(DslInspect::class)
                if (markerType != null && markerType != Annotation::class.asClassName()) {
                    addAnnotation(markerType)
                }
            }

            addFunction(typePrefix.replaceFirstChar { it.lowercase(Locale.getDefault()) }) {
                if (typeVariables.isNotEmpty()) {
                    addTypeVariables(typeVariables)
                }
                addModifiers(KModifier.INLINE)
                addAnnotation(ClassName("kotlin", "OptIn")) {
                    addMember("%T::class", ClassName("kotlin.contracts", "ExperimentalContracts"))
                }
                addParameter("initializer", parameterizedParentBuilderType.asLambdaReceiver())
                addCode {
                    addStatement("kotlin.contracts.contract { callsInPlace(initializer, kotlin.contracts.InvocationKind.EXACTLY_ONCE) }")
                    addStatement("return %T().apply(initializer).build()", parameterizedParentBuilderType)
                }
                returns(parameterizedParentType)
            }
        }.writeTo(parentClazz, codeGenerator)
    }

    private fun TypeSpecBuilder.addFlavoredInfixBlocks(
        parameter: Parameter,
        parameterClazz: T,
        builderType: ClassName,
        infixCollisions: MutableSet<String>,
    ) {
        val parentClassName = parameterClazz.asClassName()
        val parentName = parentClassName.simpleName
        val subclasses = parameterClazz.getSealedSubclasses()

        val targetTypeVariables = parameterClazz.getTypeParameters()

        val nodeTypeInterfaceName = "${parentName}Type"
        val doAddrecievers = nodeTypeInterfaceName !in infixCollisions
        infixCollisions.add(nodeTypeInterfaceName)
        if (doAddrecievers) {
            addInterface(nodeTypeInterfaceName) {
                addModifiers(KModifier.SEALED)
                if (targetTypeVariables.isNotEmpty()) {
                    addTypeVariables(targetTypeVariables)
                }

                for (subclass in subclasses) {
                    val subclassName = subclass.asClassName()
                    val tokenName = "${subclassName.simpleName}Token"

                    if (targetTypeVariables.isNotEmpty()) {
                        addClass(tokenName) {
                            addTypeVariables(targetTypeVariables)
                            addSuperinterface(ClassName("", nodeTypeInterfaceName).parameterizedBy(targetTypeVariables))
                        }
                    } else {
                        addObject(tokenName) {
                            addSuperinterface(ClassName("", nodeTypeInterfaceName))
                        }
                    }
                }
            }
        }

        for (subclass in subclasses) {
            val subclassName = subclass.asClassName()
            val typeVariableNames = subclass.getTypeParameters()
            val subclassBuilderType = subclassName.withBuilderSuffix()
            val parameterizedSubclassBuilderType =
                if (typeVariableNames.isNotEmpty()) {
                    subclassBuilderType.parameterizedBy(typeVariableNames)
                } else {
                    subclassBuilderType
                }

            val subclassSimpleName = subclassName.simpleName

            val cleanedPropertyName =
                if (subclassSimpleName.endsWith(parentName, ignoreCase = true) &&
                    subclassSimpleName.length > parentName.length
                ) {
                    subclassSimpleName.removeSuffix(parentName).replaceFirstChar { it.lowercase(Locale.getDefault()) }
                } else {
                    subclassSimpleName.replaceFirstChar { it.lowercase(Locale.getDefault()) }
                }

            val tokenClassName =
                ClassName(
                    builderType.packageName,
                    builderType.simpleNames + nodeTypeInterfaceName + "${subclassSimpleName}Token",
                )
            val propertyType = if (targetTypeVariables.isNotEmpty()) tokenClassName.parameterizedBy(targetTypeVariables) else tokenClassName

            if (doAddrecievers) {
                addProperty(cleanedPropertyName, propertyType) {
                    if (targetTypeVariables.isNotEmpty()) {
                        initializer("%T()", tokenClassName.parameterizedBy(targetTypeVariables))
                    } else {
                        initializer("%T", tokenClassName)
                    }
                }
            }

            val functionName =
                if (parameter.collectionType != null) {
                    parameter.collectionType.singular
                } else {
                    parameter.name
                }

            addFunction(functionName) {
                if (typeVariableNames.isNotEmpty()) {
                    addTypeVariables(typeVariableNames)
                }
                addModifiers(KModifier.INLINE, KModifier.INFIX)
                addAnnotation(Suppress::class) {
                    addMember("%S", "UNCHECKED_CAST")
                }
                addAnnotation(ClassName("kotlin", "OptIn")) {
                    addMember("%T::class", ClassName("kotlin.contracts", "ExperimentalContracts"))
                }

                receiver(propertyType)

                addParameter("initializer", parameterizedSubclassBuilderType.asLambdaReceiver())
                if (parameter.collectionType != null) {
                    val rawElementType = (parameter.typeName as ParameterizedTypeName).typeArguments.first()
                    val elementType =
                        if (rawElementType is ParameterizedTypeName) {
                            val classVars = parameter.typeParameters!!
                            val typeArgs =
                                rawElementType.typeArguments.mapIndexed { index, arg ->
                                    if (arg is WildcardTypeName) {
                                        if (arg.outTypes == STAR.outTypes) {
                                            val type = classVars[index].let { TypeVariableName(it.name + index, it.bounds, it.variance) }
                                            addTypeVariable(type)
                                            type
                                        } else {
                                            val type = classVars[index].let { TypeVariableName(it.name + index, arg.outTypes, it.variance) }
                                            addTypeVariable(type)
                                            type
                                        }
                                    } else {
                                        arg
                                    }
                                }
                            rawElementType.rawType.parameterizedBy(typeArgs)
                        } else {
                            rawElementType
                        }
                    addCode {
                        addStatement(
                            "kotlin.contracts.contract { callsInPlace(initializer, kotlin.contracts.InvocationKind.EXACTLY_ONCE) }",
                        )
                        addStatement("val result = %T().apply(initializer).build()", parameterizedSubclassBuilderType)
                        val parentBuilderLabel = builderType.simpleName

                        addStatement(
                            "val nodeResult = result as %T",
                            elementType,
                        )
                        addStatement(
                            "this@%L.%L = this@%L.%L?.plus(nodeResult) ?: %L(nodeResult)",
                            parentBuilderLabel,
                            parameter.name,
                            parentBuilderLabel,
                            parameter.name,
                            parameter.collectionType.createFunction,
                        )
                    }
                } else {
                    addCode {
                        addStatement(
                            "kotlin.contracts.contract { callsInPlace(initializer, kotlin.contracts.InvocationKind.EXACTLY_ONCE) }",
                        )
                        addStatement("val result = %T().apply(initializer).build()", parameterizedSubclassBuilderType)
                        val parentBuilderLabel = builderType.simpleName
                        addStatement("this@%L.%L = result as %T", parentBuilderLabel, parameter.name, parameter.typeName)
                    }
                }
            }
        }
    }
}
