package com.faendir.kotlin.autodsl

import com.faendir.kotlin.autodsl.parameter.ParameterFactory
import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.joinToCode
import io.github.enjoydambience.kotlinbard.addClass
import io.github.enjoydambience.kotlinbard.addFunction
import io.github.enjoydambience.kotlinbard.addProperty
import io.github.enjoydambience.kotlinbard.buildFile

const val DEFAULTS_BITFLAGS_FIELD_NAME = "_defaultsBitFlags"

class DslGenerator(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
    private val resolver: Resolver
) {
    private val annotationType = resolver.getClassDeclarationByName<Annotation>()!!.asStarProjectedType()
    private val parameterFactory = ParameterFactory(resolver)

    private fun error(reason: String, clazz: KSClassDeclaration) {
        logger.error("@AutoDsl can't be applied to $clazz: $reason", clazz)
    }


    fun process(): List<KSAnnotated> {
        return resolver.getClassesWithAnnotation(AutoDsl::class.java.name).flatMap { processClass(it, it.getAnnotationTypeProperty(AutoDsl::dslMarker)) }
    }

    private fun processClass(clazz: KSClassDeclaration, markerType: KSType?): List<KSAnnotated> {
        when (clazz.classKind) {
            ClassKind.INTERFACE -> error("must not be an interface", clazz)
            ClassKind.ENUM_CLASS -> error("must not be an enum class", clazz)
            ClassKind.ENUM_ENTRY -> error("must not be an enum entry", clazz)
            ClassKind.OBJECT -> error("must not be an object", clazz)
            ClassKind.ANNOTATION_CLASS -> {
                val deferred = resolver.getClassesWithAnnotation(clazz.qualifiedName!!.asString()).flatMap { processClass(it, markerType) }
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
    private fun generate(clazz: KSClassDeclaration, markerType: KSType?): Boolean {
        if (clazz.isAbstract()) {
            error("must not be abstract", clazz)
            return false
        }
        val constructors = clazz.getConstructors().filter { it.isPublic() || it.isInternal() }
        if (constructors.isEmpty()) {
            error("must have at least one public or internal constructor", clazz)
            return false
        }
        val constructor = constructors.firstOrNull { it.hasAnnotation<AutoDslConstructor>() }
            ?: clazz.primaryConstructor
            ?: constructors.first()
        if (!constructor.validate()) {
            //defer processing
            return false
        }
        val parameters = parameterFactory.get(constructor.parameters)
        val builderType = clazz.asStarProjectedType().asTypeName().withBuilderSuffix()
        buildFile(clazz.normalizedPackageName, "${clazz.simpleName.asString()}Dsl") {
            addClass(builderType.simpleName) {
                addProperty(DEFAULTS_BITFLAGS_FIELD_NAME, INT, KModifier.PRIVATE) {
                    mutable(true)
                    initializer("-1")
                }
                addProperties(parameters.map { it.getProperty() })
                addFunctions(parameters.flatMap { it.additionalFunctions() })
                addFunction("build") {
                    returns(clazz.asClassName())
                    parameters.filter { it.isMandatory }.forEach { addStatement("checkNotNull(%1L)·{ \"%1L must be assigned.\" }", it.name) }
                    if (parameters.any { it.hasDefault }) {
                        addStatement(
                            "return %T::class.java.getConstructor(%L, %T::class.java, Class.forName(%S)).newInstance(%L, %L, null)",
                            clazz.asClassName(),
                            parameters.map { it.getClassStatement() }.joinToCode(", "),
                            INT,
                            "kotlin.jvm.internal.DefaultConstructorMarker",
                            parameters.map { it.getPassToConstructorStatement(false) }.joinToCode(", "),
                            DEFAULTS_BITFLAGS_FIELD_NAME
                        )
                    } else {
                        addStatement(
                            "return %T(%L)",
                            clazz.asClassName(),
                            parameters.map { it.getPassToConstructorStatement(true) }.joinToCode(", ")
                        )
                    }
                }
                addAnnotation(DslInspect::class)
                if (markerType != null && markerType != annotationType) {
                    addAnnotation(markerType.asClassName())
                }
            }
            addFunction(clazz.simpleName.getShortName().decapitalize()) {
                addParameter("initializer", builderType.asLambdaReceiver())
                addStatement("return %T().apply(initializer).build()", builderType)
                returns(clazz.asClassName())
            }
        }.writeTo(clazz.containingFile!!, codeGenerator)
        return true
    }

}