package com.faendir.kotlin.autodsl.parameter

import com.faendir.kotlin.autodsl.asLambdaReceiver
import com.faendir.kotlin.autodsl.nonnull
import com.faendir.kotlin.autodsl.withBuilderSuffix
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.FunSpec
import io.github.enjoydambience.kotlinbard.buildFunction

class NestedDslParameter(parameter: KSValueParameter, index: Int) : Parameter(parameter, index) {
    override fun additionalFunctions(): List<FunSpec> = listOf(buildFunction(name) {
        val builderType = typeName.withBuilderSuffix()
        addParameter("initializer", builderType.asLambdaReceiver())
        addStatement("val result = %T().apply(initializer).build()", builderType)
        addStatement("%L = result", name)
        addStatement("return result")
        returns(typeName.nonnull)
        build()
    })
}