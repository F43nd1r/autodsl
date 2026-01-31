package com.faendir.kotlin.autodsl

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class AutoDslRequired(
    val group: String,
)
