package com.faendir.kotlin.autodsl

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName

@Suppress("UNCHECKED_CAST")
val <T : TypeName> T.nonnull: T
    get() = copy(nullable = false) as T

fun TypeName.toRawType(): ClassName = when (this) {
    is ParameterizedTypeName -> this.rawType
    is ClassName -> this
    is WildcardTypeName -> this.inTypes.firstOrNull()?.toRawType() ?: this.outTypes.first().toRawType()
    is LambdaTypeName -> ClassName("kotlin", "Function${(if (receiver != null) 1 else 0) + parameters.size}")
    else -> throw IllegalArgumentException("Unsupported conversion to raw type from $this")
}

fun ClassName.withBuilderSuffix() = ClassName(packageName, "${simpleName}Builder")

fun TypeName.withBuilderSuffix() = toRawType().withBuilderSuffix()

fun TypeName.asLambdaReceiver() = LambdaTypeName.get(receiver = this, returnType = Unit::class.asClassName())