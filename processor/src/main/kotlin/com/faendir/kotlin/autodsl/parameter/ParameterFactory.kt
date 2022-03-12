package com.faendir.kotlin.autodsl.parameter

import com.shadow.pluralize.singularize
import com.shadow.pluralize.utils.Plurality
import com.faendir.kotlin.autodsl.*
import com.faendir.kotlin.autodsl.parameter.CollectionType.ListType
import com.faendir.kotlin.autodsl.parameter.CollectionType.SetType
import com.squareup.kotlinpoet.asClassName
import kotlin.reflect.KClass

class ParameterFactory<A, T : A, C : A, P : A>(private val resolver: SourceInfoResolver<A, T, C, P>) {
    private val set = Set::class.asClassName()
    private val list = List::class.asClassName()
    private val collection = Collection::class.asClassName()
    private val iterable = Iterable::class.asClassName()

    fun getParameters(constructor: C): List<Parameter> = resolver.run {
        return constructor.getParameters().withIndex().map { (index, parameter) ->
            val type = parameter.getTypeName()
            val rawType = type.toRawType()
            val (hasNestedDsl, collectionType) = when (rawType) {
                set -> parameter.hasAnnotatedTypeArgument(AutoDsl::class) to SetType(findSingular(parameter, index))
                list, collection, iterable -> parameter.hasAnnotatedTypeArgument(AutoDsl::class) to ListType(findSingular(parameter, index))
                else -> (parameter.getTypeDeclaration()?.hasAnnotation(AutoDsl::class) == true) to null
            }
            Parameter(
                type,
                parameter.getName(),
                parameter.getDoc(),
                parameter.hasDefault(),
                parameter.getAnnotationProperty(AutoDslRequired::class, AutoDslRequired::group),
                index,
                hasNestedDsl,
                collectionType
            )
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