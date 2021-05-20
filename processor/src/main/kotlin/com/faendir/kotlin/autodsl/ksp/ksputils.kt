package com.faendir.kotlin.autodsl

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Light check without type resolution. A positive result does not guarantee equality
 *
 * @return false if this is not equal to annotation
 */
fun <T : Annotation> KSAnnotation.couldBe(annotation: KClass<T>): Boolean = shortName.asString() == annotation.simpleName


/**
 * Heavy check with type resolution
 *
 * @return true if this is equal to annotation
 */
fun <T : Annotation> KSAnnotation.isEqualTo(annotation: KClass<T>) =
    annotationType.resolve().declaration.qualifiedName?.asString() == annotation.java.name

fun <T : Annotation> KSAnnotated.findAnnotation(annotation: KClass<T>): KSAnnotation? {
    return annotations.filter { it.couldBe(annotation) }.firstOrNull { it.isEqualTo(annotation) }
}

val KSDeclaration.normalizedPackageName
    get() = packageName.asString().takeIf { it != "<root>" } ?: ""

fun KSTypeReference.asTypeName() = resolve().asTypeName()

fun KSType.asTypeName(): TypeName {
    var name: TypeName = asClassName()
    if (arguments.isNotEmpty()) {
        name = (name as ClassName).parameterizedBy(arguments.map { it.type!!.asTypeName() })
    }
    return if (isMarkedNullable) name.copy(true) else name
}

fun KSType.asClassName(): ClassName {
    return ClassName(declaration.normalizedPackageName, declaration.simpleName.asString())
}

