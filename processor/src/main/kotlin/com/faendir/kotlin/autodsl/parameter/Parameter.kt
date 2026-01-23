package com.faendir.kotlin.autodsl.parameter

import com.squareup.kotlinpoet.TypeName

class Parameter<T>(
    val typeName: TypeName,
    val name: String,
    val doc: String?,
    val hasDefault: Boolean,
    requiredGroup: String?,
    val index: Int,
    val type: T?,
    val collectionType: CollectionType?,
) {
    val isMandatory = requiredGroup != null || !hasDefault && !typeName.isNullable
    val group = requiredGroup ?: (name + index)
    val hasNestedDsl = type != null
}

sealed class CollectionType(val createFunction: String, val convertFunction: String, val singular: String) {
    class ListType(singular: String) : CollectionType("listOf", "toList", singular)
    class SetType(singular: String) : CollectionType("setOf", "toSet", singular)
}