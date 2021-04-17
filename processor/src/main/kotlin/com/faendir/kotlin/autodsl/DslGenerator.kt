package com.faendir.kotlin.autodsl

import com.faendir.kotlin.autodsl.parameter.Parameter
import com.faendir.kotlin.autodsl.parameter.ParameterFactory
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isInternal
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode
import kotlin.jvm.internal.DefaultConstructorMarker

class DslGenerator(private val logger: KSPLogger, private val codeGenerator: CodeGenerator, private val resolver: Resolver) {
    private val annotationType = resolver.getClassDeclarationByName<Annotation>()!!.asStarProjectedType()
    private val parameterFactory = ParameterFactory(resolver)

    fun generate(clazz: KSClassDeclaration) {
        if (clazz.isAbstract()) {
            logger.error("@AutoDsl can't be applied to $clazz: must not be abstract", clazz)
            return
        }
        val constructors = clazz.getConstructors().filter { it.isPublic() || it.isInternal() }
        if (constructors.isEmpty()) {
            logger.error("@AutoDsl can't be applied to $clazz: must have at least one public or internal constructor", clazz)
            return
        }
        val constructor = constructors
            .firstOrNull { constructor -> constructor.hasAnnotation<AutoDslConstructor>() }
            ?: clazz.primaryConstructor ?: constructors.first()
        val parameters = parameterFactory.get(constructor.parameters)
        val builderType = clazz.asStarProjectedType().toTypeName().withBuilderSuffix()
        FileSpec.builder(clazz.packageName.asString(), "${clazz.simpleName.getShortName()}Dsl")
            .addType(generateBuilder(builderType, parameters, clazz))
            .addFunction(generateDslEntryPointFunction(clazz, builderType))
            .build()
            .writeTo(clazz.containingFile!!, codeGenerator)
    }

    private fun generateDslEntryPointFunction(clazz: KSClassDeclaration, builderType: ClassName): FunSpec {
        return FunSpec.builder(clazz.simpleName.getShortName().decapitalize())
            .addParameter("initializer", LambdaTypeName.get(receiver = builderType, returnType = Unit::class.asClassName()))
            .addCode("return %T().apply(initializer).build()", builderType)
            .returns(clazz.toClassName())
            .build()
    }

    private fun generateBuilder(builderType: ClassName, parameters: List<Parameter>, clazz: KSClassDeclaration): TypeSpec {
        val classBuilder = TypeSpec.classBuilder(builderType.simpleName)
            .addProperty(PropertySpec.builder(DEFAULTS_BITFLAGS_FIELD_NAME, INT, KModifier.PRIVATE).mutable(true).initializer("-1").build())
            .addProperties(parameters.map { it.getProperty() })
            .addFunction(generateBuildFunction(clazz, parameters))
            .addFunctions(parameters.flatMap { it.additionalFunctions() })
        val dslMarkerClass = clazz.getAnnotationTypeProperty(AutoDsl::dslMarker)
        if (dslMarkerClass != annotationType) {
            classBuilder.addAnnotation(dslMarkerClass.toClassName())
        }
        return classBuilder.build()
    }

    private fun generateBuildFunction(clazz: KSClassDeclaration, parameters: List<Parameter>): FunSpec {
        return FunSpec.builder("build")
            .returns(clazz.toClassName())
            .apply {
                if (parameters.any { it.hasDefault }) {
                    addCode(
                        "return %T::class.java.getConstructor(%L, %T::class.java, %T::class.java).newInstance(%L, %L, null)",
                        clazz.toClassName(),
                        parameters.map { it.getClassStatement() }.joinToCode(", "),
                        INT,
                        DefaultConstructorMarker::class.asClassName(),
                        parameters.map { it.getPassToConstructorStatement(false) }.joinToCode(", "),
                        DEFAULTS_BITFLAGS_FIELD_NAME
                    )
                } else {
                    addCode(
                        "return %T(%L)",
                        clazz.toClassName(),
                        parameters.map { it.getPassToConstructorStatement(true) }.joinToCode(", ")
                    )
                }
            }.build()
    }
}