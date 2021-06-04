package com.faendir.kotlin.autodsl
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FUNCTION)
annotation class DslMandatory(val group: String = "")
