package com.faendir.kotlin.autodsl

import com.faendir.kotlin.autodsl.parameter.Parameter
import com.faendir.kotlin.autodsl.parameter.ParameterFactory
import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*

class DslGenerator(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
    private val resolver: Resolver
) {
    private val annotationType = resolver.getClassDeclarationByName<Annotation>()!!.asStarProjectedType()
    private val parameterFactory = ParameterFactory(resolver)


    fun process(): List<KSAnnotated> {
        return resolver.getClassesWithAnnotation(AutoDsl::class.java.name).flatMap { clazz ->
            processClass(clazz, clazz.getAnnotationTypeProperty(AutoDsl::dslMarker))
        }
    }

    private fun processClass(clazz: KSClassDeclaration, markerType: KSType?): List<KSAnnotated> {
        when (clazz.classKind) {
            ClassKind.INTERFACE -> {
                logger.error("@AutoDsl can't be applied to $clazz: must not be an interface", clazz)
            }
            ClassKind.ENUM_CLASS -> {
                logger.error("@AutoDsl can't be applied to $clazz: must not be an enum class", clazz)
            }
            ClassKind.ENUM_ENTRY -> {
                logger.error("@AutoDsl can't be applied to $clazz: must not be an enum entry", clazz)
            }
            ClassKind.OBJECT -> {
                logger.error("@AutoDsl can't be applied to $clazz: must not be an object", clazz)
            }
            ClassKind.ANNOTATION_CLASS -> {
                val deferred = resolver.getClassesWithAnnotation(clazz.qualifiedName!!.asString()).flatMap {
                    processClass(it, markerType)
                }
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
            logger.error("@AutoDsl can't be applied to $clazz: must not be abstract", clazz)
            return false
        }
        val constructors = clazz.getConstructors().filter { it.isPublic() || it.isInternal() }
        if (constructors.isEmpty()) {
            logger.error(
                "@AutoDsl can't be applied to $clazz: must have at least one public or internal constructor",
                clazz
            )
            return false
        }
        val constructor = constructors
            .firstOrNull { constructor -> constructor.hasAnnotation<AutoDslConstructor>() }
            ?: clazz.primaryConstructor ?: constructors.first()
        if (!constructor.validate()) {
            //defer processing
            return false
        }
        val parameters = parameterFactory.get(constructor.parameters)
        val builderType = clazz.asStarProjectedType().toTypeName().withBuilderSuffix()
        FileSpec.builder(clazz.normalizedPackageName, "${clazz.simpleName.getShortName()}Dsl")
            .addType(generateBuilder(builderType, markerType, parameters, clazz))
            .addFunction(generateDslEntryPointFunction(clazz, builderType))
            .build()
            .writeTo(clazz.containingFile!!, codeGenerator)
        return true
    }

    private fun generateDslEntryPointFunction(clazz: KSClassDeclaration, builderType: ClassName): FunSpec {
        return FunSpec.builder(clazz.simpleName.getShortName().decapitalize())
            .addParameter(
                "initializer",
                LambdaTypeName.get(receiver = builderType, returnType = Unit::class.asClassName())
            )
            .addCode("return %T().apply(initializer).build()", builderType)
            .returns(clazz.toClassName())
            .build()
    }

    private fun generateBuilder(
        builderType: ClassName,
        markerType: KSType?,
        parameters: List<Parameter>,
        clazz: KSClassDeclaration
    ): TypeSpec {
        val classBuilder = TypeSpec.classBuilder(builderType.simpleName)
            .addProperty(
                PropertySpec.builder(DEFAULTS_BITFLAGS_FIELD_NAME, INT, KModifier.PRIVATE)
                    .mutable(true)
                    .initializer("-1")
                    .build()
            )
            .addProperties(parameters.map { it.getProperty() })
            .addFunction(generateBuildFunction(clazz, parameters))
            .addFunctions(parameters.flatMap { it.additionalFunctions() })
            .addAnnotation(DslInspect::class)
        if (markerType != null && markerType != annotationType) {
            classBuilder.addAnnotation(markerType.toClassName())
        }
        return classBuilder.build()
    }

    private fun generateBuildFunction(clazz: KSClassDeclaration, parameters: List<Parameter>): FunSpec {
        val builder = FunSpec.builder("build")
            .returns(clazz.toClassName())
        parameters.filter { it.isMandatory }
            .forEach { builder.addStatement("checkNotNull(%1L)·{ \"%1L must be assigned.\" }", it.name) }
        if (parameters.any { it.hasDefault }) {
            builder.addStatement(
                "return %T::class.java.getConstructor(%L, %T::class.java, Class.forName(%S)).newInstance(%L, %L, null)",
                clazz.toClassName(),
                parameters.map { it.getClassStatement() }.joinToCode(", "),
                INT,
                "kotlin.jvm.internal.DefaultConstructorMarker",
                parameters.map { it.getPassToConstructorStatement(false) }.joinToCode(", "),
                DEFAULTS_BITFLAGS_FIELD_NAME
            )
        } else {
            builder.addStatement(
                "return %T(%L)",
                clazz.toClassName(),
                parameters.map { it.getPassToConstructorStatement(true) }.joinToCode(", ")
            )
        }
        return builder.build()
    }
}