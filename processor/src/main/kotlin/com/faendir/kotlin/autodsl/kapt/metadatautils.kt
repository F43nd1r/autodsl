@file:OptIn(KotlinPoetMetadataPreview::class)

package com.faendir.kotlin.autodsl.kapt

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.metadata.*
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmVariance

@OptIn(KotlinPoetMetadataPreview::class)
fun ImmutableKmClass.asClassName() = ClassName.bestGuess(this.name.replace("/", "."))

fun ImmutableKmType.asTypeName(): TypeName = when (val kmClassifier = classifier) {
    is KmClassifier.Class -> ClassName.bestGuess(kmClassifier.name.replace("/", "."))
    is KmClassifier.TypeAlias -> ClassName.bestGuess(kmClassifier.name.replace("/", "."))
    else -> throw IllegalArgumentException()
}.let { name -> if(arguments.isNotEmpty()) name.parameterizedBy(arguments.map { it.asTypeName() }) else name }

fun ImmutableKmTypeProjection.asTypeName(): TypeName = when (variance) {
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