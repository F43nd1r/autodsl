package com.faendir.kotlin.autodsl.parameter

import com.faendir.kotlin.autodsl.DEFAULTS_BITFLAGS_FIELD_NAME
import com.faendir.kotlin.autodsl.toNonNull
import com.faendir.kotlin.autodsl.toNullable
import com.faendir.kotlin.autodsl.toRawType
import com.faendir.kotlin.autodsl.toTypeName
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import kotlin.properties.Delegates

abstract class Parameter(parameter: KSValueParameter, private val index: Int) {
    val type = parameter.type.resolve()
    val typeName = type.toTypeName()
    val name = parameter.name?.asString() ?: "var$index"
    val hasDefault = parameter.hasDefault

    fun getClassStatement() = CodeBlock.of("%T::class.java", typeName.toRawType().toNonNull())

    fun getPassToConstructorStatement(checkNullity: Boolean) = CodeBlock.of(if (!checkNullity || type.isMarkedNullable) "%L" else "%L!!", name)

    fun getProperty() = PropertySpec.builder(name, typeName.toNullable())
        .mutable(true)
        .delegate("%1T.observable(null)·{·_, _, _·-> %2L = %2L and %3L }", Delegates::class.asClassName(), DEFAULTS_BITFLAGS_FIELD_NAME, (1 shl index).inv())
        .build()

    open fun additionalFunctions() = emptyList<FunSpec>()
}