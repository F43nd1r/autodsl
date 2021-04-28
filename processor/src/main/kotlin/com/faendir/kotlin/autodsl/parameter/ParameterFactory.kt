package com.faendir.kotlin.autodsl.parameter

import com.faendir.kotlin.autodsl.AutoDsl
import com.faendir.kotlin.autodsl.hasAnnotation
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter

class ParameterFactory(resolver: Resolver) {
    private val listType = resolver.getClassDeclarationByName<List<*>>()!!.asStarProjectedType()
    private val setType = resolver.getClassDeclarationByName<Set<*>>()!!.asStarProjectedType()

    fun get(parameters: Collection<KSValueParameter>): List<Parameter> {
        return parameters.withIndex().map { (index, parameter) ->
            val type = parameter.type.resolve()
            when {
                type.starProjection() == setType && type.hasAnnotatedTypeArgument<AutoDsl>() -> {
                    NestedDslSetParameter(parameter, index)
                }
                type.starProjection().isAssignableFrom(listType) && type.hasAnnotatedTypeArgument<AutoDsl>() -> {
                    NestedDslListParameter(parameter, index)
                }
                type.declaration.hasAnnotation<AutoDsl>() -> {
                    NestedDslParameter(parameter, index)
                }
                else -> {
                    StandardParameter(parameter, index)
                }
            }
        }
    }

    private inline fun <reified T : Annotation> KSType.hasAnnotatedTypeArgument(): Boolean =
        arguments.first().type!!.resolve().declaration.hasAnnotation<T>()
}