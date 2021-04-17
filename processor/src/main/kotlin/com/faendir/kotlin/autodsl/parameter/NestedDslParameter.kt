package com.faendir.kotlin.autodsl.parameter

import com.faendir.kotlin.autodsl.toNonNull
import com.faendir.kotlin.autodsl.withBuilderSuffix
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.asClassName

class NestedDslParameter(parameter: KSValueParameter, index: Int) : Parameter(parameter, index) {
    override fun additionalFunctions(): List<FunSpec> {
        val builderType = typeName.withBuilderSuffix()
        return listOf(
            FunSpec.builder(name)
                .addParameter("initializer", LambdaTypeName.get(receiver = builderType, returnType = Unit::class.asClassName()))
                .addStatement("val result = %T().apply(initializer).build()", builderType)
                .addStatement("%L = result", name)
                .addStatement("return result")
                .returns(typeName.toNonNull())
                .build()
        )
    }
}