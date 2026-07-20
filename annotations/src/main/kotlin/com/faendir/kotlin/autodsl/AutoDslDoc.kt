package com.faendir.kotlin.autodsl

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS)
annotation class AutoDslDoc(
    val kDoc: String,
)
