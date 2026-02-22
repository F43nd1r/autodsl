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
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode
import io.github.enjoydambience.kotlinbard.TypeSpecBuilder
import io.github.enjoydambience.kotlinbard.addAnnotation
import io.github.enjoydambience.kotlinbard.addClass
import io.github.enjoydambience.kotlinbard.addFunction
import io.github.enjoydambience.kotlinbard.addProperty
import io.github.enjoydambience.kotlinbard.buildFile
import io.github.enjoydambience.kotlinbard.codeFmt
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
        clazz: T,
        markerType: ClassName?,
    ): Boolean {
        if (clazz.isAbstract()) {
            error("must not be abstract", clazz)
            return false
        }
        val constructors = clazz.getConstructors().filter { it.isAccessible() }
        if (constructors.isEmpty()) {
            error("must have at least one public or internal constructor", clazz)
            return false
        }
        val constructor =
            constructors.firstOrNull { it.hasAnnotation(AutoDslConstructor::class) }
                ?: clazz.getPrimaryConstructor()
                ?: constructors.first()
        if (!constructor.isValid()) {
            // defer processing
            return false
        }
        val parameters = parameterFactory.getParameters(constructor)
        val type = clazz.asClassName()
        val builderType = type.withBuilderSuffix()
        val bitFieldIndices = 0..parameters.size / 32
        val typePrefix = type.simpleNames.joinToString("")
        buildFile(type.packageName, "${typePrefix}Dsl") {
            addClass(builderType.simpleName) {
                if (parameters.any { it.hasDefault }) {
                    for (i in bitFieldIndices) {
                        addProperty(DEFAULTS_BITFLAGS_FIELD_NAME + i, INT, KModifier.PRIVATE) {
                            mutable(true)
                            initializer("-1")
                        }
                    }
                }
                for (parameter in parameters) {
                    addParameterProperty(parameter, type)
                    addParameterBuilderStyleSetter(parameter, builderType, type)
                    if (parameter.collectionType != null) {
                        addParameterBuilderStyleVarargSetter(parameter, builderType, type)
                        if (parameter.hasNestedDsl) {
                            addParameterNestedAdder(parameter)
                        }
                    } else if (parameter.hasNestedDsl) {
                        addParameterNestedSetter(parameter)
                    }
                }
                addFunction("build") {
                    returns(type)
                    parameters.filter { it.isMandatory }.groupBy { it.group }.forEach { (_, parameters) ->
                        addStatement(
                            "check(%L)·{ \"%L·must·be·assigned.\" }",
                            parameters.map { "%L != null".codeFmt(it.name) }.joinToCode(" || "),
                            parameters.joinToString(separator = ",·", prefix = if (parameters.size > 1) "One·of·" else "") { it.name },
                        )
                    }
                    if (parameters.any { it.hasDefault }) {
                        addStatement(
                            "return %T::class.java.getConstructor(%L, %L, %L).newInstance(%L, %L, null)",
                            type,
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
                        )
                    } else {
                        addStatement(
                            "return %T(%L)",
                            type,
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
                addParameter("initializer", builderType.asLambdaReceiver())
                addStatement("return %T().apply(initializer).build()", builderType)
                returns(type)
            }
        }.writeTo(clazz, codeGenerator)
        return true
    }

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
            val elementType = (parameter.typeName as ParameterizedTypeName).typeArguments.first()
            val nestedBuilderType = elementType.withBuilderSuffix()
            addParameter("initializer", nestedBuilderType.asLambdaReceiver())
            addStatement("val result = %T().apply(initializer).build()", nestedBuilderType)
            addStatement("%1L = %1L?.plus(result) ?: %2L(result)", parameter.name, parameter.collectionType.createFunction)
            addStatement("return result")
            returns(elementType.nonnull)
        }

    private fun TypeSpecBuilder.addParameterBuilderStyleVarargSetter(
        parameter: Parameter,
        builderType: ClassName,
        type: ClassName,
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
        addKdoc("@see %T.%L", type, parameter.name)
    }

    private fun TypeSpecBuilder.addParameterBuilderStyleSetter(
        parameter: Parameter,
        builderType: ClassName,
        type: ClassName,
    ) = addFunction("with${parameter.name.replaceFirstChar { it.uppercase() }}") {
        returns(builderType)
        addParameter(parameter.name, parameter.typeName)
        addStatement("this.%1L·= %1L", parameter.name)
        addStatement("return this")
        parameter.doc?.let { addKdoc(it) }
        addKdoc("@see %T.%L", type, parameter.name)
    }

    private fun TypeSpecBuilder.addParameterProperty(
        parameter: Parameter,
        type: ClassName,
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
        addKdoc("@see %T.%L", type, parameter.name)
    }
}
