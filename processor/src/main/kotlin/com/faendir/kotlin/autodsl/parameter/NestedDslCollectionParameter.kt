package com.faendir.kotlin.autodsl.parameter

import com.cesarferreira.pluralize.singularize
import com.faendir.kotlin.autodsl.toNonNull
import com.faendir.kotlin.autodsl.toTypeName
import com.faendir.kotlin.autodsl.withBuilderSuffix
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.asClassName

open class NestedDslCollectionParameter(parameter: KSValueParameter, index: Int, private val createFunction: String) : Parameter(parameter, index) {
    override fun additionalFunctions(): List<FunSpec> {
        val dslType = type.arguments.first().type!!.toTypeName()
        val builderType = dslType.withBuilderSuffix()
        return listOf(
            FunSpec.builder(name.singularize())
                .addParameter("initializer", LambdaTypeName.get(receiver = builderType, returnType = Unit::class.asClassName()))
                .addStatement("val result = %T().apply(initializer).build()", builderType)
                .addStatement("%1L = %1L?.plus(result) ?: %2L(result)", name, createFunction)
                .addStatement("return result")
                .returns(dslType.toNonNull())
                .build()
        )
    }
}

class NestedDslListParameter(parameter: KSValueParameter, index: Int) : NestedDslCollectionParameter(parameter, index, "listOf")

class NestedDslSetParameter(parameter: KSValueParameter, index: Int) : NestedDslCollectionParameter(parameter, index, "setOf")