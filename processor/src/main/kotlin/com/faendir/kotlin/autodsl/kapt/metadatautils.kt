@file:OptIn(KotlinPoetMetadataPreview::class)

package com.faendir.kotlin.autodsl.kapt

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.ImmutableKmType
import com.squareup.kotlinpoet.metadata.ImmutableKmTypeProjection
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
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
        type!!.asTypeName()
    }
    KmVariance.IN -> {
        WildcardTypeName.consumerOf(type!!.asTypeName())
    }
    KmVariance.OUT -> {
        WildcardTypeName.producerOf(type!!.asTypeName())
    }
}