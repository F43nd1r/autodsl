package com.faendir.kotlin.autodsl.parameter

import com.faendir.kotlin.autodsl.asLambdaReceiver
import com.faendir.kotlin.autodsl.nonnull
import com.faendir.kotlin.autodsl.withBuilderSuffix
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeName
import io.github.enjoydambience.kotlinbard.buildFunction

class NestedDslParameter(typeName: TypeName, name: String, doc: String?, hasDefault: Boolean, index: Int) : Parameter(typeName, name, doc, hasDefault, index) {
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