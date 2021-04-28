package com.faendir.kotlin.autodsl.parameter

import com.cesarferreira.pluralize.singularize
import com.cesarferreira.pluralize.utils.Plurality
import com.faendir.kotlin.autodsl.*
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.FunSpec
import io.github.enjoydambience.kotlinbard.buildFunction

open class NestedDslCollectionParameter(
    private val parameter: KSValueParameter,
    index: Int,
    private val createFunction: String
) :
    Parameter(parameter, index) {
    override fun additionalFunctions(): List<FunSpec> = listOf(buildFunction(
        when {
            parameter.hasAnnotation<AutoDslSingular>() -> parameter.getAnnotationProperty(AutoDslSingular::value)
            name.length < 3 -> name
            else -> name.singularize(Plurality.CouldBeEither)
        }
    ) {
        val dslType = type.arguments.first().type!!.asTypeName()
        val builderType = dslType.withBuilderSuffix()
        addParameter("initializer", builderType.asLambdaReceiver())
        addStatement("val result = %T().apply(initializer).build()", builderType)
        addStatement("%1L = %1L?.plus(result) ?: %2L(result)", name, createFunction)
        addStatement("return result")
        returns(dslType.nonnull)
    })
}

class NestedDslListParameter(parameter: KSValueParameter, index: Int) :
    NestedDslCollectionParameter(parameter, index, "listOf")

class NestedDslSetParameter(parameter: KSValueParameter, index: Int) :
    NestedDslCollectionParameter(parameter, index, "setOf")