package com.faendir.kotlin.autodsl.ksp

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Variance
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import kotlin.reflect.KClass

/**
 * Light check without type resolution. A positive result does not guarantee equality
 *
 * @return false if this is not equal to annotation
 */
fun <T : Annotation> KSAnnotation.couldBe(annotation: KClass<T>): Boolean = shortName.asString() == annotation.simpleName


/**
 * Heavy check with type resolution
 *
 * @return true if this is equal to annotation
 */
fun <T : Annotation> KSAnnotation.isEqualTo(annotation: KClass<T>) =
    annotationType.resolve().declaration.qualifiedName?.asString() == annotation.java.name

fun <T : Annotation> KSAnnotated.findAnnotation(annotation: KClass<T>): KSAnnotation? {
    return annotations.filter { it.couldBe(annotation) }.firstOrNull { it.isEqualTo(annotation) }
}

fun KSTypeReference.asTypeName() = resolve().asTypeName()

fun KSType.asTypeName(): TypeName {
    var name: TypeName = asClassName()
    if (declaration.qualifiedName?.asString()?.matches(Regex("kotlin\\.Function\\d+")) == true) {
        val arguments = arguments.mapNotNullTo(mutableListOf()) { it.type?.asTypeName() }
        val receiver = if (annotations.any { it.couldBe(ExtensionFunctionType::class) }) arguments.removeFirst() else null
        val returnType = arguments.removeLast()
        name = LambdaTypeName.get(receiver = receiver, returnType = returnType, parameters = arguments.toTypedArray())
    } else if (arguments.isNotEmpty()) {
        name = (name as ClassName).parameterizedBy(arguments.map {
            when (it.variance) {
                Variance.STAR -> STAR
                Variance.INVARIANT -> it.type!!.asTypeName()
                Variance.COVARIANT -> WildcardTypeName.producerOf(it.type!!.asTypeName())
                Variance.CONTRAVARIANT -> WildcardTypeName.consumerOf(it.type!!.asTypeName())
            }
        })
    }
    return if (isMarkedNullable) name.copy(nullable = true) else name
}

fun KSDeclaration.asClassName() =
    ClassName(packageName.asString(), generateSequence({ this }, { it.parentDeclaration }).map { it.simpleName.asString() }.toList().reversed())

fun KSType.asClassName(): ClassName = declaration.asClassName()

