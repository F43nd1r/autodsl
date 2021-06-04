package com.faendir.kotlin.autodsl.kapt

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.enjoydambience.kotlinbard.nullable
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

fun ClassName.asFqName() = FqName.fromSegments(listOf(packageName) + simpleNames)

fun ClassId.asClassName() = ClassName(packageFqName.asString(), relativeClassName.asString())

fun ClassName.mapToKotlin() = JavaToKotlinClassMap.mapJavaToKotlin(asFqName())?.asClassName() ?: this

fun TypeName.mapToKotlin(): TypeName = when (this) {
    is ClassName -> mapToKotlin()
    is ParameterizedTypeName -> rawType.mapToKotlin().parameterizedBy(typeArguments.map { it.mapToKotlin() })
    is WildcardTypeName -> if (inTypes.isEmpty()) WildcardTypeName.producerOf(outTypes.first().mapToKotlin())
    else WildcardTypeName.consumerOf(inTypes.first().mapToKotlin())
    else -> this
}