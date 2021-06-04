package com.faendir.kotlin.autodsl

import com.faendir.kotlin.autodsl.parameter.ParameterFactory
import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.*
import io.github.enjoydambience.kotlinbard.addClass
import io.github.enjoydambience.kotlinbard.addFunction
import io.github.enjoydambience.kotlinbard.addProperty
import io.github.enjoydambience.kotlinbard.buildFile
import java.util.*
import kotlin.jvm.internal.DefaultConstructorMarker

const val DEFAULTS_BITFLAGS_FIELD_NAME = "_defaultsBitFlags"

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


    fun process(): List<T> {
        return resolver.getClassesWithAnnotation(AutoDsl::class)
            .flatMap { processClass(it, resolver.run { it.getAnnotationTypeProperty(AutoDsl::class, AutoDsl::dslMarker) }) }
    }

    private fun processClass(clazz: T, markerType: ClassName?): List<T> = resolver.run {
        when (clazz.getClassKind()) {
            ClassKind.INTERFACE -> error("must not be an interface", clazz)
            ClassKind.ENUM_CLASS -> error("must not be an enum class", clazz)
            ClassKind.ENUM_ENTRY -> error("must not be an enum entry", clazz)
            ClassKind.OBJECT -> error("must not be an object", clazz)
            ClassKind.ANNOTATION_CLASS -> {
                val deferred = resolver.getClassesWithAnnotation(clazz).flatMap { processClass(it, markerType) }
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
        emptyList()
    }

    /**
     * returns true if class generation was successful
     */
    private fun generate(clazz: T, markerType: ClassName?): Boolean = resolver.run {
        if (clazz.isAbstract()) {
            error("must not be abstract", clazz)
            return false
        }
        val constructors = clazz.getConstructors().filter { it.isAccessible() }
        if (constructors.isEmpty()) {
            error("must have at least one public or internal constructor", clazz)
            return false
        }
        val constructor = constructors.firstOrNull { it.hasAnnotation(AutoDslConstructor::class) }
            ?: clazz.getPrimaryConstructor()
            ?: constructors.first()
        if (!constructor.isValid()) {
            //defer processing
            return false
        }
        val parameters = parameterFactory.getParameters(constructor)
        val type = clazz.asClassName()
        val builderType = type.withBuilderSuffix()
        buildFile(type.packageName, "${type.simpleName}Dsl") {
            addClass(builderType.simpleName) {
                addProperty(DEFAULTS_BITFLAGS_FIELD_NAME, INT, KModifier.PRIVATE) {
                    mutable(true)
                    initializer("-1")
                }
                addProperties(parameters.map { it.getProperty(type) })
                addFunctions(parameters.map { it.getBuilderFunction(type, builderType) })
                addFunctions(parameters.flatMap { it.additionalFunctions(type, builderType) })
                addFunction("build") {
                    returns(type)
                    parameters.filter { it.isMandatory }.forEach { addStatement("checkNotNull(%1L)·{ \"%1L must be assigned.\" }", it.name) }
                    if (parameters.any { it.hasDefault }) {
                        val (markerExpression, markerParameter) = if (kotlinVersion.isAtLeast(1, 5)) {
                            "%T::class.java" to DefaultConstructorMarker::class.asClassName()
                        } else {
                            "Class.forName(%S)" to DefaultConstructorMarker::class.java.name
                        }
                        addStatement(
                            "return %T::class.java.getConstructor(%L, %T::class.java, $markerExpression).newInstance(%L, %L, null)",
                            type,
                            parameters.map { it.getClassStatement() }.joinToCode(", "),
                            INT,
                            markerParameter,
                            parameters.map { it.getPassToConstructorStatement(false) }.joinToCode(", "),
                            DEFAULTS_BITFLAGS_FIELD_NAME
                        )
                    } else {
                        addStatement(
                            "return %T(%L)",
                            type,
                            parameters.map { it.getPassToConstructorStatement(true) }.joinToCode(", ")
                        )
                    }
                }
                addAnnotation(DslInspect::class)
                if (markerType != null && markerType != Annotation::class.asClassName()) {
                    addAnnotation(markerType)
                }
            }
            addFunction(type.simpleName.replaceFirstChar { it.lowercase(Locale.getDefault()) }) {
                addParameter("initializer", builderType.asLambdaReceiver())
                addStatement("return %T().apply(initializer).build()", builderType)
                returns(type)
            }
        }.writeTo(clazz, codeGenerator)
        return true
    }

}