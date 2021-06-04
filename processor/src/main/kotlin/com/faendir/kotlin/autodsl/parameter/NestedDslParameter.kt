package com.faendir.kotlin.autodsl.parameter

import com.faendir.kotlin.autodsl.asLambdaReceiver
import com.faendir.kotlin.autodsl.nonnull
import com.faendir.kotlin.autodsl.withBuilderSuffix
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeName
import io.github.enjoydambience.kotlinbard.buildFunction

class NestedDslParameter(typeName: TypeName, name: String, doc: String?, hasDefault: Boolean, requiredGroup: String?, index: Int) :
    Parameter(typeName, name, doc, hasDefault, requiredGroup, index) {
    override fun additionalFunctions(referencedType: TypeName, builderType: TypeName): List<FunSpec> = listOf(buildFunction(name) {
        val nestedBuilderType = typeName.withBuilderSuffix()
        addParameter("initializer", nestedBuilderType.asLambdaReceiver())
        addStatement("val result = %T().apply(initializer).build()", nestedBuilderType)
        addStatement("%L = result", name)
        addStatement("return result")
        returns(typeName.nonnull)
    })
}