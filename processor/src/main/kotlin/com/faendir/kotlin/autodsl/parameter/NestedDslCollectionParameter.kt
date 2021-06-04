package com.faendir.kotlin.autodsl.parameter

import com.faendir.kotlin.autodsl.asLambdaReceiver
import com.faendir.kotlin.autodsl.nonnull
import com.faendir.kotlin.autodsl.withBuilderSuffix
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import io.github.enjoydambience.kotlinbard.buildFunction

open class NestedDslCollectionParameter(
    typeName: TypeName,
    name: String,
    doc: String,
    hasDefault: Boolean,
    private val singular: String,
    index: Int,
    private val createFunction: String,
) :
    Parameter(typeName, name, doc, hasDefault, index) {
    override fun additionalFunctions(): List<FunSpec> = listOf(buildFunction(singular) {
        val dslType = (typeName as ParameterizedTypeName).typeArguments.first()
        val builderType = dslType.withBuilderSuffix()
        addParameter("initializer", builderType.asLambdaReceiver())
        addStatement("val result = %T().apply(initializer).build()", builderType)
        addStatement("%1L = %1L?.plus(result) ?: %2L(result)", name, createFunction)
        addStatement("return result")
        returns(dslType.nonnull)
    })
}

class NestedDslListParameter(typeName: TypeName, name: String, doc: String, hasDefault: Boolean, singular: String, index: Int) :
    NestedDslCollectionParameter(typeName, name, doc, hasDefault, singular, index, "listOf")

class NestedDslSetParameter(typeName: TypeName, name: String, doc: String, hasDefault: Boolean, singular: String, index: Int) :
    NestedDslCollectionParameter(typeName, name, doc, hasDefault, singular, index, "setOf")