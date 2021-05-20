package com.faendir.kotlin.autodsl

import com.squareup.kotlinpoet.*

@Suppress("UNCHECKED_CAST")
val <T : TypeName> T.nonnull: T
    get() = copy(nullable = false) as T

fun TypeName.toRawType(): ClassName = when (this) {
    is ParameterizedTypeName -> this.rawType
    is ClassName -> this
    else -> throw IllegalArgumentException()
}

fun ClassName.withBuilderSuffix() = ClassName(packageName, "${simpleName}Builder")

fun TypeName.withBuilderSuffix() = toRawType().withBuilderSuffix()

fun TypeName.asLambdaReceiver() = LambdaTypeName.get(receiver = this, returnType = Unit::class.asClassName())

inline fun <reified T : Annotation> ParameterSpec.hasAnnotation(): Boolean {
    val typeName = T::class.asTypeName()
    return this.annotations.any { it.typeName == typeName }
}