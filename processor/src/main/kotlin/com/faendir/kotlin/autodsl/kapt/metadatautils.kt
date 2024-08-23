@file:OptIn(KotlinPoetMetadataPreview::class)

package com.faendir.kotlin.autodsl.kapt

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import io.github.enjoydambience.kotlinbard.nullable
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmType
import kotlinx.metadata.KmTypeProjection
import kotlinx.metadata.KmVariance
import kotlinx.metadata.isNullable
import kotlinx.metadata.jvm.annotations

fun KmClass.asClassName() = ClassName.bestGuess(this.name.replace("/", "."))

fun KmType.asTypeName(): TypeName {
    var name = when (val kmClassifier = classifier) {
        is KmClassifier.Class -> {
            if (kmClassifier.name.matches(Regex("kotlin/Function\\d+"))) {
                val arguments = arguments.mapNotNullTo(mutableListOf()) { it.type?.asTypeName() }
                val receiver =
                    if (annotations.any { it.className.replace("/", ".") == ExtensionFunctionType::class.qualifiedName }) arguments.removeFirst() else null
                val returnType = arguments.removeLast()
                LambdaTypeName.get(receiver = receiver, returnType = returnType, parameters = arguments.toTypedArray())
            } else {
                ClassName.bestGuess(kmClassifier.name.replace("/", "."))
            }
        }

        is KmClassifier.TypeAlias -> ClassName.bestGuess(kmClassifier.name.replace("/", "."))
        else -> throw IllegalArgumentException()
    }
    if (arguments.isNotEmpty() && name is ClassName) {
        name = name.parameterizedBy(arguments.map { it.asTypeName() })
    }
    if (isNullable) name = name.nullable
    return name
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