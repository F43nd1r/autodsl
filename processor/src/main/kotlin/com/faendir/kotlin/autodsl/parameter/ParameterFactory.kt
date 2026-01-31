package com.faendir.kotlin.autodsl.parameter

import com.shadow.pluralize.singularize
import com.shadow.pluralize.utils.Plurality
import com.faendir.kotlin.autodsl.*
import com.faendir.kotlin.autodsl.parameter.CollectionType.ListType
import com.faendir.kotlin.autodsl.parameter.CollectionType.SetType
import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.asClassName

class ParameterFactory<A, T : A, C : A, P : A>(private val resolver: SourceInfoResolver<A, T, C, P>) {
    private val classes: Set<T> = resolver.run {
        getClassesWithAnnotation(AutoDsl::class).flatMapTo(mutableSetOf()) {
            when (it.getClassKind()) {
                ClassKind.ANNOTATION_CLASS -> getClassesWithAnnotation(it)
                else -> listOf(it)
            }
        }
    }
    private val set = Set::class.asClassName()
    private val list = List::class.asClassName()
    private val collection = Collection::class.asClassName()
    private val iterable = Iterable::class.asClassName()

    fun getParameters(constructor: C): List<Parameter<T>> = resolver.run {
        constructor.getParameters().mapIndexed { index, parameter ->
            val typeName = parameter.getTypeName()
            val rawType = typeName.toRawType()
            val (type, collectionType) = when (rawType) {
                set -> parameter.typeArgumentIfAnnotated() to SetType(findSingular(parameter, index))
                list, collection, iterable -> parameter.typeArgumentIfAnnotated() to ListType(findSingular(parameter, index))
                else -> (parameter.getTypeDeclaration()?.takeIf { it in classes }) to null
            }
            Parameter(
                typeName,
                parameter.getName(),
                parameter.getDoc(),
                parameter.hasDefault(),
                parameter.getAnnotationProperty(AutoDslRequired::class, AutoDslRequired::group),
                index,
                type,
                collectionType,
            )
        }
    }

    private fun P.typeArgumentIfAnnotated(): T? = resolver.run {
        getTypeArguments().firstOrNull()?.takeIf { it in classes }
    }

    private fun findSingular(parameter: P, index: Int): String = resolver.run {
        when {
            parameter.hasAnnotation(AutoDslSingular::class) -> parameter.getAnnotationProperty(AutoDslSingular::class, AutoDslSingular::value)
            parameter.getName().length < 3 -> parameter.getName()
            else -> parameter.getName().singularize(Plurality.CouldBeEither)
        } ?: "var$index"
    }
}