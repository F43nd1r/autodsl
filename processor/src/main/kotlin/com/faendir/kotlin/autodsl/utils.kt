package com.faendir.kotlin.autodsl

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

fun KSTypeReference.toTypeName() = resolve().toTypeName()

fun KSType.toTypeName(): TypeName {
    var name: TypeName = toClassName()
    if (arguments.isNotEmpty()) {
        name = (name as ClassName).parameterizedBy(arguments.map { it.type!!.toTypeName() })
    }
    return if (isMarkedNullable) name.copy(true) else name
}

fun KSType.toClassName(): ClassName {
    return ClassName(declaration.normalizedPackageName, declaration.simpleName.asString())
}

fun KSClassDeclaration.toClassName(): ClassName {
    return ClassName(normalizedPackageName, simpleName.asString())
}

fun FileSpec.writeTo(source: KSFile, codeGenerator: CodeGenerator) {
    codeGenerator.createNewFile(Dependencies(true, source), packageName, name).writer().use { writeTo(it) }
}

fun TypeName.toNullable() = copy(true)

fun TypeName.toNonNull() = copy(false)

fun TypeName.toRawType(): ClassName = when (this) {
    is ParameterizedTypeName -> this.rawType
    is ClassName -> this
    else -> throw IllegalArgumentException()
}

inline fun <reified T : Annotation> KSAnnotated.hasAnnotation() = hasAnnotation(T::class)

fun <T : Annotation> KSAnnotated.hasAnnotation(annotation: KClass<T>): Boolean {
    return annotations.any { it.annotationType.resolve().declaration.qualifiedName!!.asString() == annotation.java.name }
}

inline fun <reified T : Annotation> KSAnnotated.getAnnotation() = getAnnotation(T::class)

fun <T : Annotation> KSAnnotated.getAnnotation(annotation: KClass<T>): KSAnnotation {
    return annotations.first { it.annotationType.resolve().declaration.qualifiedName!!.asString() == annotation.java.name }
}

inline fun <reified T : Annotation> KSAnnotated.getAnnotationTypeProperty(property: KProperty1<T, KClass<*>>): KSType? {
    return getAnnotation<T>().arguments.first { it.name?.asString() == property.name }.value as? KSType?
}

inline fun <reified T : Annotation, R> KSAnnotated.getAnnotationProperty(property: KProperty1<T, R>): R {
    return getAnnotation<T>().arguments.first { it.name?.asString() == property.name }.value as R
}

const val DEFAULTS_BITFLAGS_FIELD_NAME = "_defaultsBitFlags"

fun ClassName.withBuilderSuffix() = ClassName(packageName, "${simpleName}Builder")

fun TypeName.withBuilderSuffix() = toRawType().withBuilderSuffix()

fun Resolver.getClassesWithAnnotation(name: String) = getSymbolsWithAnnotation(name).filterIsInstance<KSClassDeclaration>()

val KSDeclaration.normalizedPackageName
    get() = packageName.asString().takeIf { it != "<root>"} ?: ""