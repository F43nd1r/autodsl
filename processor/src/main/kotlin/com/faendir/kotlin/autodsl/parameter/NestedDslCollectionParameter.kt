package com.faendir.kotlin.autodsl.parameter

import com.faendir.kotlin.autodsl.asLambdaReceiver
import com.faendir.kotlin.autodsl.nonnull
import com.faendir.kotlin.autodsl.withBuilderSuffix
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import io.github.enjoydambience.kotlinbard.buildFunction

open class NestedDslCollectionParameter(
    typeName: TypeName,
    name: String,
    doc: String?,
    hasDefault: Boolean,
    private val singular: String,
    index: Int,
    private val createFunction: String,
    private val convertFunction: String,
) :
    Parameter(typeName, name, doc, hasDefault, index) {
    override fun additionalFunctions(referencedType: TypeName, builderType: TypeName): List<FunSpec> {
        val dslType = (typeName as ParameterizedTypeName).typeArguments.first()
        return listOf(buildFunction(singular) {
            val nestedBuilderType = dslType.withBuilderSuffix()
            addParameter("initializer", nestedBuilderType.asLambdaReceiver())
            addStatement("val result = %T().apply(initializer).build()", nestedBuilderType)
            addStatement("%1L = %1L?.plus(result) ?: %2L(result)", name, createFunction)
            addStatement("return result")
            returns(dslType.nonnull)
        }, buildFunction("with${name.replaceFirstChar { it.uppercase() }}") {
            returns(builderType)
            addParameter(name, dslType, KModifier.VARARG)
            addStatement("return apply·{ this.%1L·= %1L.%2L() }", name, convertFunction)
            doc?.let { addKdoc(it) }
            addKdoc("@see %T.%L", referencedType, name)
        })
    }
}

class NestedDslListParameter(typeName: TypeName, name: String, doc: String?, hasDefault: Boolean, singular: String, index: Int) :
    NestedDslCollectionParameter(typeName, name, doc, hasDefault, singular, index, "listOf", "toList")

class NestedDslSetParameter(typeName: TypeName, name: String, doc: String?, hasDefault: Boolean, singular: String, index: Int) :
    NestedDslCollectionParameter(typeName, name, doc, hasDefault, singular, index, "setOf", "toSet")