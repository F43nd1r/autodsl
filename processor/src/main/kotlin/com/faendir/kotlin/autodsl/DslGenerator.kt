package com.faendir.kotlin.autodsl

import com.faendir.kotlin.autodsl.parameter.CollectionType
import com.faendir.kotlin.autodsl.parameter.Parameter
import com.faendir.kotlin.autodsl.parameter.ParameterFactory
import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.ANY
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
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode
import io.github.enjoydambience.kotlinbard.FileSpecBuilder
import io.github.enjoydambience.kotlinbard.TypeSpecBuilder
import io.github.enjoydambience.kotlinbard.addAnnotation
import io.github.enjoydambience.kotlinbard.addClass
import io.github.enjoydambience.kotlinbard.addCode
import io.github.enjoydambience.kotlinbard.addFunction
import io.github.enjoydambience.kotlinbard.addKdoc
import io.github.enjoydambience.kotlinbard.addParameter
import io.github.enjoydambience.kotlinbard.addProperty
import io.github.enjoydambience.kotlinbard.buildFile
import io.github.enjoydambience.kotlinbard.codeFmt
import io.github.enjoydambience.kotlinbard.controlFlow
import io.github.enjoydambience.kotlinbard.getter
import io.github.enjoydambience.kotlinbard.`if`
import io.github.enjoydambience.kotlinbard.nullable
import io.github.enjoydambience.kotlinbard.primaryConstructor
import io.github.enjoydambience.kotlinbard.setter
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
        if (entity.isAbstract()) {
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
                    addTypeVariable(typeParam)
                }
                if (parameters.any { it.hasDefault }) {
                    for (i in bitFieldIndices) {
                        addProperty(DEFAULTS_BITFLAGS_FIELD_NAME + i, INT, KModifier.PRIVATE) {
                            mutable(true)
                            initializer("-1")
                        }
                    }
                }
                if (parameters.any { it.hasNestedDsl && it.collectionType is CollectionType.MapType }) {
                    addClass("BuilderMapProxy") {
                        addModifiers(KModifier.PRIVATE)
                        val kType = TypeVariableName("K")
                        val vType = TypeVariableName("V")
                        val bType = TypeVariableName("B")
                        addTypeVariable(kType)
                        addTypeVariable(vType)
                        addTypeVariable(bType)

                        val delegateField = "delegate"
                        val factoryField = "builderFactory"
                        val executorField = "builderExecutor"
                        val setterField = "setter"

                        primaryConstructor {
                            addParameter(delegateField, Map::class.asClassName().parameterizedBy(kType, vType))
                            addParameter(factoryField, LambdaTypeName.get(returnType = bType))
                            addParameter(executorField, LambdaTypeName.get(parameters = arrayOf(bType), returnType = vType))
                            addParameter(
                                setterField,
                                LambdaTypeName.get(
                                    parameters = arrayOf(Map::class.asClassName().parameterizedBy(kType, vType)),
                                    returnType = Unit::class.asClassName(),
                                ),
                            )
                        }

                        addProperty(delegateField, Map::class.asClassName().parameterizedBy(kType, vType), KModifier.PRIVATE) {
                            initializer(delegateField)
                        }
                        addProperty(factoryField, LambdaTypeName.get(returnType = bType), KModifier.PRIVATE) {
                            initializer(factoryField)
                        }
                        addProperty(executorField, LambdaTypeName.get(parameters = arrayOf(bType), returnType = vType), KModifier.PRIVATE) {
                            initializer(executorField)
                        }
                        addProperty(
                            setterField,
                            LambdaTypeName.get(
                                parameters = arrayOf(Map::class.asClassName().parameterizedBy(kType, vType)),
                                returnType = Unit::class.asClassName(),
                            ),
                            KModifier.PRIVATE,
                        ) {
                            initializer(setterField)
                        }

                        addSuperinterface(Map::class.asClassName().parameterizedBy(kType, vType), "delegate")

                        addFunction("put") {
                            addParameter("key", kType)
                            val blockType = bType.asLambdaReceiver()
                            addParameter("block", blockType)
                            addCode {
                                addStatement("val builder = %L()", factoryField)
                                addStatement("builder.block()")
                                addStatement("val result = %L(builder)", executorField)
                                addStatement("%1L(%2L + (key to result))", setterField, delegateField)
                            }
                        }
                        addFunction("put") {
                            addParameter("key", kType)
                            addParameter("value", vType)
                            addCode {
                                addStatement("%1L(%2L + (key to value))", setterField, delegateField)
                            }
                        }
                    }
                }
                if (parameters.any { !it.hasNestedDsl && it.collectionType is CollectionType.MapType }) {
                    addClass("MapProxy") {
                        addModifiers(KModifier.PRIVATE)
                        val kType = TypeVariableName("K")
                        val vType = TypeVariableName("V")
                        addTypeVariable(kType)
                        addTypeVariable(vType)

                        val delegateField = "delegate"
                        val setterField = "setter"

                        primaryConstructor {
                            addParameter(delegateField, Map::class.asClassName().parameterizedBy(kType, vType))
                            addParameter(
                                setterField,
                                LambdaTypeName.get(
                                    parameters = arrayOf(Map::class.asClassName().parameterizedBy(kType, vType)),
                                    returnType = Unit::class.asClassName(),
                                ),
                            )
                        }

                        addProperty(delegateField, Map::class.asClassName().parameterizedBy(kType, vType), KModifier.PRIVATE) {
                            initializer(delegateField)
                        }
                        addProperty(
                            setterField,
                            LambdaTypeName.get(
                                parameters = arrayOf(Map::class.asClassName().parameterizedBy(kType, vType)),
                                returnType = Unit::class.asClassName(),
                            ),
                            KModifier.PRIVATE,
                        ) {
                            initializer(setterField)
                        }

                        addSuperinterface(Map::class.asClassName().parameterizedBy(kType, vType), "delegate")

                        addFunction("put") {
                            addParameter("key", kType)
                            addParameter("value", vType)
                            addCode {
                                addStatement("%1L(%2L + (key to value))", setterField, delegateField)
                            }
                        }
                    }
                }
                val operatorCollisions = mutableSetOf<TypeName>()
                for (parameter in parameters) {
                    addParameterProperty(parameter, entityClass)
                    addParameterBuilderStyleSetter(parameter, builderType, entityClass)
                    if (parameter.collectionType != null) {
                        addParameterBuilderStyleVarargSetter(parameter, builderType, entityClass)
                        if (parameter.hasNestedDsl) {
                            addParameterNestedAdder(parameter)
                        }
                        if (operatorCollisions.add(parameter.typeName)) {
                            addParameterOperatorAdder(parameter)
                        }
                    } else if (parameter.hasNestedDsl) {
                        addParameterNestedSetter(parameter)
                    }
                }
                addFunction("build") {
                    returns(entityType)
                    parameters.filter { it.isMandatory }.groupBy { it.group }.forEach { (_, parameters) ->
                        addStatement(
                            "check(%L)·{ \"%L·must·be·assigned.\" }",
                            parameters
                                .map {
                                    "%L != null".codeFmt(
                                        if (it.collectionType is CollectionType.MapType) "_${it.name}" else it.name,
                                    )
                                }.joinToCode(" || "),
                            parameters.joinToString(separator = ",·", prefix = if (parameters.size > 1) "One·of·" else "") { it.name },
                        )
                    }
                    if (parameters.any { it.hasDefault }) {
                        addStatement(
                            "return %T::class.java.getConstructor(%L, %L, %L).newInstance(%L, %L, null)",
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
                                        it.collectionType is CollectionType.MapType -> "_%L"
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
                        )
                    } else {
                        addStatement(
                            "return %T(%L)",
                            entityClass,
                            parameters
                                .map {
                                    (if (it.typeName.isNullable) "%L" else "%L!!").codeFmt(
                                        if (it.collectionType is CollectionType.MapType) "_${it.name}" else it.name,
                                    )
                                }.joinToCode(),
                        )
                    }
                }
                addAnnotation(DslInspect::class)
                if (markerType != null && markerType != Annotation::class.asClassName()) {
                    addAnnotation(markerType)
                }
                addKdoc {
                    entity.getAnnotationProperty(AutoDslDoc::class, AutoDslDoc::kDoc)?.let { add("%L", it) }
                }
            }
            addFunction(typePrefix.replaceFirstChar { it.lowercase(Locale.getDefault()) }) {
                addModifiers(KModifier.INLINE)
                for (typeParam in entityTypeParameters) {
                    addTypeVariable(typeParam)
                }
                addParameter("initializer", builderType.asLambdaReceiver())
                addStatement("return %T().apply(initializer).build()", builderType)
                returns(entityType)
            }
            addFunction("add") {
                addModifiers(KModifier.INLINE)
                for (typeParam in entityTypeParameters) {
                    addTypeVariable(typeParam)
                }
                receiver(Collection::class.asClassName().withMutablePrefix().parameterizedBy(entityType))
                addParameter("builder", builderType.asLambdaReceiver())
                addStatement("return this@add.add(%T().apply(builder).build())", builderType)
                returns(Boolean::class.asClassName())
            }
            addFunction("plusAssign") {
                addModifiers(KModifier.INLINE, KModifier.OPERATOR)
                for (typeParam in entityTypeParameters) {
                    addTypeVariable(typeParam)
                }
                receiver(Collection::class.asClassName().withMutablePrefix().parameterizedBy(entityType))
                addParameter("builder", builderType.asLambdaReceiver())
                addStatement("this@plusAssign += %T().apply(builder).build()", builderType)
            }
            addFunction("put") {
                addModifiers(KModifier.INLINE)
                val key = TypeVariableName("KEY")
                addTypeVariable(key)
                for (typeParam in entityTypeParameters) {
                    addTypeVariable(typeParam)
                }
                receiver(Map::class.asClassName().withMutablePrefix().parameterizedBy(key, entityType))
                addParameter("key", key)
                addParameter("builder", builderType.asLambdaReceiver())
                addStatement("return this@put.put(key, %T().apply(builder).build())", builderType)
                returns(entityType.nullable)
            }
            addFunction("set") {
                addModifiers(KModifier.INLINE, KModifier.OPERATOR)
                val key = TypeVariableName("KEY")
                addTypeVariable(key)
                for (typeParam in entityTypeParameters) {
                    addTypeVariable(typeParam)
                }
                receiver(Map::class.asClassName().withMutablePrefix().parameterizedBy(key, entityType))
                addParameter("key", key)
                addParameter("builder", builderType.asLambdaReceiver())
                addStatement("this@set.put(key, builder)")
            }
        }.writeTo(entity, codeGenerator)
        return true
    }

    private fun ClassName.parameterize(typeParameters: List<TypeVariableName>) =
        if (typeParameters.isEmpty()) this else parameterizedBy(typeParameters)

    private fun TypeSpecBuilder.addParameterNestedSetter(parameter: Parameter) =
        addFunction(parameter.name) {
            addModifiers(KModifier.INLINE)
            val nestedBuilderType = parameter.typeName.withBuilderSuffix()
            addParameter("initializer", nestedBuilderType.asLambdaReceiver())
            addStatement("val result = %T().apply(initializer).build()", nestedBuilderType)
            addStatement("%L = result", parameter.name)
            addStatement("return result")
            returns(parameter.typeName.nonnull)
        }

    private fun TypeSpecBuilder.addParameterNestedAdder(parameter: Parameter) =
        addFunction(parameter.collectionType!!.singular) {
            addModifiers(KModifier.INLINE)
            if (parameter.collectionType is CollectionType.MapType) {
                val keyType = (parameter.typeName as ParameterizedTypeName).typeArguments.getOrNull(0) ?: ANY
                val valueType = parameter.typeName.typeArguments.getOrNull(1) ?: ANY
                val nestedBuilderType = valueType.withBuilderSuffix()
                addParameter("key", keyType)
                addParameter("initializer", nestedBuilderType.asLambdaReceiver())
                addStatement("val result = %T().apply(initializer).build()", nestedBuilderType)
                addStatement(
                    "%1L = %1L?.plus(key to result) ?: %2L(key to result)",
                    parameter.name,
                    parameter.collectionType.createFunction,
                )
                addStatement("return result")
                returns(valueType.nonnull)
            } else {
                val elementType = (parameter.typeName as ParameterizedTypeName).typeArguments.first()
                val nestedBuilderType = elementType.withBuilderSuffix()
                addParameter("initializer", nestedBuilderType.asLambdaReceiver())
                addStatement("val result = %T().apply(initializer).build()", nestedBuilderType)
                addStatement(
                    "%1L = %1L?.plus(result) ?: %2L(result)",
                    parameter.name,
                    parameter.collectionType.createFunction,
                )
                addStatement("return result")
                returns(elementType.nonnull)
            }
        }

    private fun TypeSpecBuilder.addParameterBuilderStyleVarargSetter(
        parameter: Parameter,
        builderType: TypeName,
        entityClass: ClassName,
    ) = addFunction("with${parameter.name.replaceFirstChar { it.uppercase() }}") {
        returns(builderType)
        if (parameter.collectionType is CollectionType.MapType) {
            val keyType =
                (parameter.typeName as ParameterizedTypeName).typeArguments[0].let {
                    if (it is WildcardTypeName) it.outTypes.first() else it
                }
            val valueType =
                parameter.typeName.typeArguments[1].let {
                    if (it is WildcardTypeName) it.outTypes.first() else it
                }
            addParameter(parameter.name, Pair::class.asClassName().parameterizedBy(keyType, valueType), KModifier.VARARG)
            addStatement("this.%1L·= %1L.%2L()", parameter.name, parameter.collectionType.convertFunction)
        } else {
            val elementType =
                (parameter.typeName as ParameterizedTypeName).typeArguments.first().let {
                    if (it is WildcardTypeName) it.outTypes.first() else it
                }
            addParameter(parameter.name, elementType, KModifier.VARARG)
            addStatement("this.%1L·= %1L.%2L()", parameter.name, parameter.collectionType!!.convertFunction)
        }
        addStatement("return this")
        addKdoc {
            parameter.doc?.let { add("%L\n\n", it) }
            add("@see %T.%L", entityClass, parameter.name)
        }
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
        addKdoc {
            parameter.doc?.let { add("%L\n\n", it) }
            add("@see %T.%L", entityClass, parameter.name)
        }
    }

    private fun TypeSpecBuilder.addParameterProperty(
        parameter: Parameter,
        entityClass: ClassName,
    ) {
        if (parameter.collectionType is CollectionType.MapType) {
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
                    }.withoutVariance() as ParameterizedTypeName
            val keyType = safeType.typeArguments[0]
            val valueType = safeType.typeArguments[1]
            val valueBuilderType = valueType.withBuilderSuffix()
            val backingFieldName = "_${parameter.name}"
            addProperty(backingFieldName, parameter.typeName.nullable, KModifier.PRIVATE) {
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
            }

            addProperty(parameter.name, parameter.typeName.nullable) {
                mutable(true)
                getter {
                    if (parameter.hasNestedDsl) {
                        addStatement(
                            "return %1T(%2L ?: emptyMap(), { %3T() }, { it.build() }) { %2L = it }",
                            ClassName("", "BuilderMapProxy").parameterizedBy(keyType, valueType, valueBuilderType),
                            backingFieldName,
                            valueBuilderType,
                        )
                    } else {
                        addStatement(
                            "return %1T(%2L ?: emptyMap()) { %2L = it }",
                            ClassName("", "MapProxy").parameterizedBy(keyType, valueType),
                            backingFieldName,
                        )
                    }
                }

                setter {
                    addParameter("value", parameter.typeName.nullable)
                    addStatement("%L = value", backingFieldName)
                }

                if (parameter.isMandatory) {
                    addAnnotation(DslMandatory::class) {
                        useSiteTarget(AnnotationSpec.UseSiteTarget.SET)
                        addMember("group = %S", parameter.group)
                    }
                }
                addKdoc {
                    parameter.doc?.let { add("%L\n\n", it) }
                    add("@see %T.%L", entityClass, parameter.name)
                }
            }
        } else {
            addProperty(parameter.name, parameter.typeName.nullable) {
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
                addKdoc {
                    parameter.doc?.let { add("%L\n\n", it) }
                    add("@see %T.%L", entityClass, parameter.name)
                }
            }
        }
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
                }.withoutVariance() as ParameterizedTypeName
        if (parameter.collectionType is CollectionType.MapType) {
            val keyType = safeType.typeArguments[0]
            val valueType = safeType.typeArguments[1]
            val blockType = valueType.withBuilderSuffix().asLambdaReceiver()
            addFunction("plus") {
                receiver(safeType.nullable)
                returns(safeType)
                addAnnotation(JvmName::class) {
                    addMember("%S", "add${parameter.collectionType.singular.replaceFirstChar { it.uppercase() }}")
                }
                addModifiers(KModifier.OPERATOR)
                addParameter("pair", Pair::class.asClassName().parameterizedBy(keyType, valueType))
                addCode {
                    controlFlow(
                        "return %L",
                        parameter.collectionType.builderFunction,
                    ) {
                        `if`("this@plus != null") {
                            addStatement("putAll(this@plus)")
                        }
                        addStatement("put(pair.first, pair.second)")
                    }
                }
            }
            addFunction("plus") {
                receiver(safeType.nullable)
                returns(safeType)
                addAnnotation(JvmName::class) {
                    addMember("%S", "addAll${parameter.collectionType.singular.replaceFirstChar { it.uppercase() }}")
                }
                addModifiers(KModifier.OPERATOR)
                addParameter("value", safeType)
                addCode {
                    controlFlow(
                        "return %L",
                        parameter.collectionType.builderFunction,
                    ) {
                        `if`("this@plus != null") {
                            addStatement("putAll(this@plus)")
                        }
                        addStatement("putAll(value)")
                    }
                }
            }
            if (parameter.hasNestedDsl) {
                addFunction("set") {
                    receiver(safeType.nullable)
                    addAnnotation(JvmName::class) {
                        addMember("%S", "putBuilder${parameter.collectionType.singular.replaceFirstChar { it.uppercase() }}")
                    }
                    addAnnotation(Suppress::class) {
                        addMember("%S", "UNCHECKED_CAST")
                    }
                    addModifiers(KModifier.OPERATOR)
                    addParameter("key", keyType)
                    addParameter("block", blockType)
                    addCode {
                        `if`("this@set is %T", ClassName("", "BuilderMapProxy").parameterizedBy(STAR, STAR, STAR)) {
                            addStatement(
                                "(this@set as %T<%T, %T, %T>).put(key, block)",
                                ClassName("", "BuilderMapProxy"),
                                keyType,
                                valueType,
                                valueType.withBuilderSuffix(),
                            )
                        }
                    }
                }
                addFunction("set") {
                    receiver(safeType.nullable)
                    addAnnotation(JvmName::class) {
                        addMember("%S", "put${parameter.collectionType.singular.replaceFirstChar { it.uppercase() }}")
                    }
                    addAnnotation(Suppress::class) {
                        addMember("%S", "UNCHECKED_CAST")
                    }
                    addModifiers(KModifier.OPERATOR)
                    addParameter("key", keyType)
                    addParameter("value", valueType)
                    addCode {
                        `if`("this@set is %T", ClassName("", "BuilderMapProxy").parameterizedBy(STAR, STAR, STAR)) {
                            addStatement(
                                "(this@set as %T<%T, %T, %T>).put(key, value)",
                                ClassName("", "BuilderMapProxy"),
                                keyType,
                                valueType,
                                valueType.withBuilderSuffix(),
                            )
                        }
                    }
                }
            } else {
                addFunction("set") {
                    receiver(safeType.nullable)
                    addAnnotation(JvmName::class) {
                        addMember("%S", "put${parameter.collectionType.singular.replaceFirstChar { it.uppercase() }}")
                    }
                    addAnnotation(Suppress::class) {
                        addMember("%S", "UNCHECKED_CAST")
                    }
                    addModifiers(KModifier.OPERATOR)
                    addParameter("key", keyType)
                    addParameter("value", valueType)
                    addCode {
                        `if`("this@set is %T", ClassName("", "MapProxy").parameterizedBy(STAR, STAR)) {
                            addStatement(
                                "(this@set as %T<%T, %T>).put(key, value)",
                                ClassName("", "MapProxy"),
                                keyType,
                                valueType,
                            )
                        }
                    }
                }
            }
        } else {
            val elementType = safeType.typeArguments.first()
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
    }
}
