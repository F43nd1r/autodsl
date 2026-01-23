package com.faendir.kotlin.autodsl

import com.faendir.kotlin.autodsl.parameter.Parameter
import com.faendir.kotlin.autodsl.parameter.ParameterFactory
import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.*
import io.github.enjoydambience.kotlinbard.*
import kotlin.contracts.ExperimentalContracts
import kotlin.jvm.internal.DefaultConstructorMarker
import kotlin.properties.Delegates

const val DEFAULTS_BITFLAGS_FIELD_NAME = "_defaultsBitField"

class DslGenerator<A, T : A, C : A>(
    private val kotlinVersion: KotlinVersion,
    private val logger: Logger<T>,
    private val codeGenerator: CodeWriter<T>,
    private val resolver: SourceInfoResolver<A, T, C, *>
) {
    private val parameterFactory = ParameterFactory(resolver)

    private fun error(reason: String, clazz: T) {
        logger.error("@AutoDsl can't be applied to $clazz: $reason", clazz)
    }


    fun process(): List<T> = resolver.run {
        getClassesWithAnnotation(AutoDsl::class).flatMap {
            processClass(
                it,
                it.getAnnotationTypeProperty(AutoDsl::class, AutoDsl::dslMarker),
                it.getAnnotationProperty(AutoDsl::class, AutoDsl::safe) ?: SafetyType.Unsafe,
            )
        }
    }

    private fun processClass(clazz: T, markerType: ClassName?, safe: SafetyType): List<T> = resolver.run {
        when (clazz.getClassKind()) {
            ClassKind.INTERFACE -> error("must not be an interface", clazz)
            ClassKind.ENUM_CLASS -> error("must not be an enum class", clazz)
            ClassKind.ENUM_ENTRY -> error("must not be an enum entry", clazz)
            ClassKind.OBJECT -> error("must not be an object", clazz)
            ClassKind.ANNOTATION_CLASS -> {
                val deferred = getClassesWithAnnotation(clazz).flatMap { processClass(it, markerType, safe) }
                if (deferred.isNotEmpty()) {
                    return deferred + clazz
                }
            }

            ClassKind.CLASS -> {
                if (!generate(clazz, markerType, safe)) {
                    return listOf(clazz)
                }
            }
        }
        emptyList()
    }

    private fun T.parameters(): List<Parameter<T>>? = resolver.run {
        if (isAbstract()) {
            error("must not be abstract", this@parameters)
            return null
        }
        val constructors = getConstructors().filter { it.isAccessible() }
        if (constructors.isEmpty()) {
            error("must have at least one public or internal constructor", this@parameters)
            return null
        }
        val constructor = constructors.firstOrNull { it.hasAnnotation(AutoDslConstructor::class) }
            ?: getPrimaryConstructor()
            ?: constructors.first()
        if (!constructor.isValid()) {
            //defer processing
            return null
        }
        return parameterFactory.getParameters(constructor)
    }

    private fun ClassName.parameterInterface(parameter: Parameter<T>): ClassName = nestedClass(parameter.group)

    /**
     * returns true if class generation was successful
     */
    private fun generate(clazz: T, markerType: ClassName?, safe: SafetyType): Boolean = resolver.run {
        val parameters = clazz.parameters() ?: return false
        val requiredParameters = parameters.filter { it.isMandatory }
        val requiredGroups = requiredParameters.groupBy { it.group }.values
        val type = clazz.asClassName()
        val builderType = type.withBuilderSuffix()
        val bitFieldIndices = 0..parameters.size / 32
        val isSafe = safe != SafetyType.Unsafe
        val builderTypeImpl = builderType.withImplSuffix()
        buildFile(type.packageName, "${type.simpleName}Dsl") {
            addImport("kotlin.contracts", "contract")
            addAnnotation(ClassName("kotlin", "OptIn")) { addMember("%T::class", ExperimentalContracts::class) }
            if (isSafe) {
                addClass(builderType) {
                    addModifiers(KModifier.SEALED)
                    if (markerType != null && markerType != Annotation::class.asClassName()) addAnnotation(markerType)
                    for (parameter in parameters) {
                        if (parameter.hasNestedDsl) {
                            if (parameter.collectionType != null) addParameterNestedAdder(parameter, builderType, true)
                            else addParameterNestedSetter(parameter, builderType, true)
                        }
                        if (!parameter.isMandatory) addProperty(parameter.name, parameter.typeName.nullable) {
                            mutable()
                            addModifiers(KModifier.ABSTRACT)
                            addKdoc(parameter, type)
                        }
                    }
                    for (group in requiredGroups) addInterface(builderType.parameterInterface(group.first())) {
                        addModifiers(KModifier.SEALED)
                        if (markerType != null && markerType != Annotation::class.asClassName())
                            addAnnotation(markerType)
                        for (parameter in group) addProperty(parameter.name, parameter.typeName.nullable) {
                            if (safe == SafetyType.Safe) mutable()
                            addKdoc(parameter, type)
                        }
                    }
                }

                for (parameter in requiredParameters) addProperty(parameter.name, parameter.typeName) {
                    mutable()
                    receiver(builderType)
                    addKdoc(parameter, type)
                    getter {
                        addAnnotation(Deprecated::class) {
                            addMember("message = %S", "")
                            addMember("level = %T.%L", DeprecationLevel::class, "HIDDEN")
                        }
                        addStatement("error(%S)", "Should not be called")
                    }
                    setter {
                        addParameter("value", parameter.typeName)
                        addReturnsContract(parameter.name, builderType.parameterInterface(parameter))
                        addStatement("when (this) { is %T -> %L = value }", builderTypeImpl, parameter.name)
                    }
                }
            }
            addClass(if (isSafe) builderTypeImpl else builderType) {
                if (isSafe) {
                    superclass(builderType)
                    for ((parameter) in requiredGroups) addSuperinterface(builderType.parameterInterface(parameter))
                }
                if (parameters.any { it.hasDefault }) {
                    for (i in bitFieldIndices) {
                        addProperty(DEFAULTS_BITFLAGS_FIELD_NAME + i, INT, KModifier.PRIVATE) {
                            mutable(true)
                            initializer("-1")
                        }
                    }
                }
                for (parameter in parameters) {
                    addParameterProperty(parameter, type, isSafe)
                    addParameterBuilderStyleSetter(parameter, builderType, type)
                    if (parameter.collectionType != null) {
                        addParameterBuilderStyleVarargSetter(parameter, builderType, type)
                        if (parameter.hasNestedDsl && !isSafe) {
                            addParameterNestedAdder(parameter, builderType, false)
                        }
                    } else if (parameter.hasNestedDsl && !isSafe) {
                        addParameterNestedSetter(parameter, builderType, false)
                    }
                }
                addFunction("build") {
                    returns(type)
                    requiredGroups.forEach { group ->
                        addStatement(
                            "check(%L)·{ \"%L·must·be·assigned.\" }",
                            group.map { "%L != null".codeFmt(it.name) }.joinToCode(" || "),
                            group.joinToString(
                                separator = ",·",
                                prefix = if (group.size > 1) "One·of·" else ""
                            ) { it.name }
                        )
                    }
                    if (parameters.any { it.hasDefault }) {
                        addStatement(
                            "return %T::class.java.getConstructor(%L, %L, %L).newInstance(%L, %L, null)",
                            type,
                            parameters.map {
                                "%T::class.%L".codeFmt(
                                    it.typeName.toRawType().nonnull,
                                    if (it.typeName.isNullable) "javaObjectType" else "java"
                                )
                            }.joinToCode(),
                            bitFieldIndices.map { "%T::class.java".codeFmt(INT) }.joinToCode(),
                            if (kotlinVersion.isAtLeast(
                                    1,
                                    5
                                )
                            ) "%T::class.java".codeFmt(DefaultConstructorMarker::class.asClassName())
                            else "Class.forName(%S)".codeFmt(DefaultConstructorMarker::class.java.name),
                            parameters.map {
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
                            bitFieldIndices.map { "%L".codeFmt(DEFAULTS_BITFLAGS_FIELD_NAME + it) }.joinToCode()
                        )
                    } else {
                        addStatement(
                            "return %T(%L)",
                            type,
                            parameters.map { (if (it.typeName.isNullable) "%L" else "%L!!").codeFmt(it.name) }
                                .joinToCode()
                        )
                    }
                }
                addAnnotation(DslInspect::class)
                if (markerType != null && markerType != Annotation::class.asClassName()) {
                    addAnnotation(markerType)
                }
            }
            addFunction(type.builderFunction) {
                addModifiers(KModifier.INLINE)
                addInitializerParameter(builderType, isSafe, parameters)
                if (isSafe) addStatement(
                    "return when(val builder = initializer(%T())) { is %T -> builder.build() }",
                    builderTypeImpl,
                    builderTypeImpl
                ) else addStatement("return %T().apply(initializer).build()", builderType)
                returns(type)
            }
        }.writeTo(clazz, codeGenerator)
        return true
    }

    private fun FunSpecBuilder.callBuilder(builderType: ClassName) = addStatement(
        "val result = %L%L(initializer)",
        builderType.packagePrefix,
        builderType.builderFunction,
    )

    private fun TypeSpecBuilder.addParameterNestedSetter(
        parameter: Parameter<T>,
        builderType: ClassName,
        isSafe: Boolean,
    ): Unit = addFunction(parameter.name) {
        addModifiers(KModifier.INLINE)
        val parameterType = parameter.typeName.toRawType()
        addInitializerParameter(parameterType.withBuilderSuffix(), isSafe, parameter.type?.parameters() ?: return)
        if (isSafe && parameter.isMandatory) addReturnsContract(
            builderType.simpleName,
            builderType.parameterInterface(parameter)
        )
        callBuilder(parameterType)
        addStatement("%L = result", parameter.name)
        addStatement("return result")
        returns(parameter.typeName.nonnull)
    }

    private fun TypeSpecBuilder.addParameterNestedAdder(
        parameter: Parameter<T>,
        builderType: ClassName,
        isSafe: Boolean,
    ): Unit = addFunction(parameter.collectionType!!.singular) {
        addModifiers(KModifier.INLINE)
        val elementType = (parameter.typeName as ParameterizedTypeName).typeArguments.first().toRawType()
        addInitializerParameter(elementType.withBuilderSuffix(), isSafe, parameter.type?.parameters() ?: return)
        if (isSafe && parameter.isMandatory) addReturnsContract(
            builderType.simpleName,
            builderType.parameterInterface(parameter)
        )
        callBuilder(elementType)
        val assignment = CodeBlock.of(
            "%1L = %1L?.plus(result) ?: %2L(result)",
            parameter.name,
            parameter.collectionType.createFunction
        )
        if (isSafe) addStatement("when (this) { is %T -> %L }", builderType.withImplSuffix(), assignment)
        else addStatement("%L", assignment)
        addStatement("return result")
        returns(elementType.nonnull)
    }

    private fun FunSpecBuilder.addReturnsContract(thisLabel: String, castAs: TypeName) =
        addStatement("contract { returns() implies (this@%L is %T) }", thisLabel, castAs)

    private fun Documentable.Builder<*>.addKdoc(parameter: Parameter<T>, type: ClassName) {
        parameter.doc?.let { addKdoc(it) }
        addKdoc("@see %T.%L", type, parameter.name)
    }

    private fun FunSpecBuilder.addInitializerParameter(
        builderType: ClassName,
        isSafe: Boolean,
        parameters: List<Parameter<T>>,
    ) = if (isSafe) {
        val type = TypeVariableName("T", parameters.filter { it.isMandatory }.distinctBy { it.group }.map {
            builderType.parameterInterface(it)
        }.ifEmpty { listOf(builderType) })
        addTypeVariable(type)
        addParameter("initializer", LambdaTypeName.get(receiver = builderType, returnType = type))
    } else addParameter("initializer", builderType.asLambdaReceiver())

    private fun TypeSpecBuilder.addParameterBuilderStyleVarargSetter(
        parameter: Parameter<T>,
        builderType: ClassName,
        type: ClassName
    ) = addFunction("with${parameter.name.replaceFirstChar { it.uppercase() }}") {
        val elementType = (parameter.typeName as ParameterizedTypeName).typeArguments.first()
            .let { if (it is WildcardTypeName) it.outTypes.first() else it }
        returns(builderType)
        addParameter(parameter.name, elementType, KModifier.VARARG)
        addStatement("this.%1L·= %1L.%2L()", parameter.name, parameter.collectionType!!.convertFunction)
        addStatement("return this")
        addKdoc(parameter, type)
    }

    private fun TypeSpecBuilder.addParameterBuilderStyleSetter(
        parameter: Parameter<T>,
        builderType: ClassName,
        type: ClassName
    ) = addFunction("with${parameter.name.replaceFirstChar { it.uppercase() }}") {
        returns(builderType)
        addParameter(parameter.name, parameter.typeName)
        addStatement("this.%1L·= %1L", parameter.name)
        addStatement("return this")
        addKdoc(parameter, type)
    }

    private fun TypeSpecBuilder.addParameterProperty(parameter: Parameter<T>, type: ClassName, isSafe: Boolean) =
        addProperty(parameter.name, parameter.typeName.nullable) {
            mutable(true)
            if (parameter.hasDefault) {
                delegate(
                    "%1T.observable(null)·{·_, _, _·-> %2L·= %2L and %3L }",
                    Delegates::class.asClassName(),
                    DEFAULTS_BITFLAGS_FIELD_NAME + (parameter.index / 32),
                    (1 shl parameter.index % 32).inv()
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
            if (isSafe) addModifiers(KModifier.OVERRIDE)
            addKdoc(parameter, type)
        }

}