package com.faendir.kotlin.autodsl

import kotlin.reflect.KClass

abstract class AnnotationFinder<NODE : Any> {
    private val annotationCache = mutableMapOf<Pair<NODE, KClass<*>>, Any?>()

    protected abstract fun <T : Annotation> NODE.getDirectAnnotation(annotation: KClass<T>): T?

    protected abstract fun NODE.getDirectAnnotations(): Iterable<NODE>

    protected abstract fun NODE.getQualifiedName(): String?

    fun <T : Annotation> NODE.findAnnotation(annotationClass: KClass<T>): T? {
        val key = this to annotationClass
        if (key in annotationCache) {
            @Suppress("UNCHECKED_CAST")
            return annotationCache[key] as T?
        }
        annotationCache[key] = null // prevent cycles

        val result =
            getDirectAnnotation(annotationClass)
                ?: getDirectAnnotations()
                    .filter {
                        val name = it.getQualifiedName()
                        name != null && !name.startsWith("kotlin.") && !name.startsWith("java.") && !name.startsWith("javax.")
                    }.firstNotNullOfOrNull { it.findAnnotation(annotationClass) }

        annotationCache[key] = result
        return result
    }
}
