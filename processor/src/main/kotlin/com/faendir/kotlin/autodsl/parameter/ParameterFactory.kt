package com.faendir.kotlin.autodsl.parameter

import com.cesarferreira.pluralize.singularize
import com.cesarferreira.pluralize.utils.Plurality
import com.faendir.kotlin.autodsl.*
import com.squareup.kotlinpoet.asClassName
import kotlin.reflect.KClass

class ParameterFactory<A, T : A, C : A, P : A>(private val resolver: SourceInfoResolver<A, T, C, P>) {
    private val setType = Set::class.asClassName()
    private val listType = List::class.asClassName()
    private val collectionType = Collection::class.asClassName()
    private val iterableType = Iterable::class.asClassName()

    fun getParameters(constructor: C): List<Parameter> = resolver.run {
        return constructor.getParameters().withIndex().map { (index, parameter) ->
            val type = parameter.getTypeName()
            val rawType = type.toRawType()
            val name = parameter.getName()
            val doc = parameter.getDoc()
            val hasDefault = parameter.hasDefault()
            val requiredGroup = parameter.getAnnotationProperty(AutoDslRequired::class, AutoDslRequired::group)
            when {
                rawType == setType && parameter.hasAnnotatedTypeArgument(AutoDsl::class) -> {
                    NestedDslSetParameter(type, name, doc, hasDefault, requiredGroup, findSingular(parameter, index), index)
                }
                (rawType == listType || rawType == collectionType || rawType == iterableType) && parameter.hasAnnotatedTypeArgument(AutoDsl::class) -> {
                    NestedDslListParameter(type, name, doc, hasDefault, requiredGroup, findSingular(parameter, index), index)
                }
                parameter.getTypeDeclaration()?.hasAnnotation(AutoDsl::class) == true -> {
                    NestedDslParameter(type, name, doc, hasDefault, requiredGroup, index)
                }
                else -> {
                    StandardParameter(type, name, doc, hasDefault, requiredGroup, index)
                }
            }
        }
    }

    private fun P.hasAnnotatedTypeArgument(annotation: KClass<out Annotation>): Boolean = resolver.run {
        return getTypeArguments().firstOrNull()?.hasAnnotation(annotation) ?: false
    }

    private fun findSingular(parameter: P, index: Int): String = resolver.run {
        when {
            parameter.hasAnnotation(AutoDslSingular::class) -> parameter.getAnnotationProperty(AutoDslSingular::class, AutoDslSingular::value)
            parameter.getName().length < 3 -> parameter.getName()
            else -> parameter.getName().singularize(Plurality.CouldBeEither)
        } ?: "var$index"
    }
}