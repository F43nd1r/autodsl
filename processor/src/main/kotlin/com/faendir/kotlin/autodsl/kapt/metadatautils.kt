@file:OptIn(KotlinPoetMetadataPreview::class)

package com.faendir.kotlin.autodsl.kapt

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.metadata.*
import io.github.enjoydambience.kotlinbard.nullable
import kotlinx.metadata.*

fun KmClass.asClassName() = ClassName.bestGuess(this.name.replace("/", "."))

fun KmType.asTypeName(): TypeName {
    val className = when (val kmClassifier = classifier) {
        is KmClassifier.Class -> ClassName.bestGuess(kmClassifier.name.replace("/", "."))
        is KmClassifier.TypeAlias -> ClassName.bestGuess(kmClassifier.name.replace("/", "."))
        else -> throw IllegalArgumentException()
    }
    var typeName = if (arguments.isNotEmpty()) className.parameterizedBy(arguments.map { it.asTypeName() }) else className
    if (isNullable) typeName = typeName.nullable
    return typeName
}

fun KmTypeProjection.asTypeName(): TypeName = when (variance) {
    null, KmVariance.INVARIANT -> {
        type?.asTypeName()?.copy(nullable = type!!.isNullable) ?: STAR
    }
    KmVariance.IN -> {
        WildcardTypeName.consumerOf(type!!.asTypeName().copy(nullable = type!!.isNullable))
    }
    KmVariance.OUT -> {
        WildcardTypeName.producerOf(type!!.asTypeName().copy(nullable = type!!.isNullable))
    }
}