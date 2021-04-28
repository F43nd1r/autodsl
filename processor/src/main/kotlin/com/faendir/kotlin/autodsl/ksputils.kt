package com.faendir.kotlin.autodsl

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Light check without type resolution. A positive result does not guarantee equality
 *
 * @return false if this is not equal to annotation
 */
private fun <T : Annotation> KSAnnotation.couldBe(annotation: KClass<T>) =
    (annotationType.element as? KSClassifierReference)?.referencedName() == annotation.simpleName


/**
 * Heavy check with type resolution
 *
 * @return true if this is equal to annotation
 */
private fun <T : Annotation> KSAnnotation.isEqualTo(annotation: KClass<T>) =
    annotationType.resolve().declaration.qualifiedName?.asString() == annotation.java.name

inline fun <reified T : Annotation> KSAnnotated.hasAnnotation() = hasAnnotation(T::class)

fun <T : Annotation> KSAnnotated.hasAnnotation(annotation: KClass<T>): Boolean {
    return annotations.filter { it.couldBe(annotation) }.any { it.isEqualTo(annotation) }
}

inline fun <reified T : Annotation> KSAnnotated.getAnnotation() = getAnnotation(T::class)

fun <T : Annotation> KSAnnotated.getAnnotation(annotation: KClass<T>): KSAnnotation {
    return annotations.filter { it.couldBe(annotation) }.first { it.isEqualTo(annotation) }
}

inline fun <reified T : Annotation> KSAnnotated.getAnnotationTypeProperty(property: KProperty1<T, KClass<*>>): KSType? {
    return getAnnotation<T>().arguments.first { it.name?.asString() == property.name }.value as? KSType?
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Annotation, R> KSAnnotated.getAnnotationProperty(property: KProperty1<T, R>): R {
    return getAnnotation<T>().arguments.first { it.name?.asString() == property.name }.value as R
}

fun Resolver.getClassesWithAnnotation(name: String) = getSymbolsWithAnnotation(name).filterIsInstance<KSClassDeclaration>()

val KSDeclaration.normalizedPackageName
    get() = packageName.asString().takeIf { it != "<root>" } ?: ""