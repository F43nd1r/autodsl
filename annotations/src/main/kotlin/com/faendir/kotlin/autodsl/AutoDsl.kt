package com.faendir.kotlin.autodsl

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS)
annotation class AutoDsl(val dslMarker: KClass<out Annotation> = Annotation::class )
