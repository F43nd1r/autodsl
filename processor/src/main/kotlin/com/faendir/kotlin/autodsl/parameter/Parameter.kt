package com.faendir.kotlin.autodsl.parameter

import com.faendir.kotlin.autodsl.DEFAULTS_BITFLAGS_FIELD_NAME
import com.faendir.kotlin.autodsl.DslMandatory
import com.faendir.kotlin.autodsl.nonnull
import com.faendir.kotlin.autodsl.toRawType
import com.squareup.kotlinpoet.*
import io.github.enjoydambience.kotlinbard.addAnnotation
import io.github.enjoydambience.kotlinbard.buildProperty
import io.github.enjoydambience.kotlinbard.nullable
import kotlin.properties.Delegates

abstract class Parameter(val typeName: TypeName, val name: String, val hasDefault: Boolean, private val index: Int) {
    val isMandatory = !hasDefault && !typeName.isNullable

    fun getClassStatement() = CodeBlock.of("%T::class.java", typeName.toRawType().nonnull)

    fun getPassToConstructorStatement(checkNullity: Boolean) = CodeBlock.of(
        when {
            typeName.isNullable -> "%L"
            checkNullity -> "%L!!"
            typeName == BOOLEAN -> "%L ?: false"
            typeName == BYTE -> "%L ?: 0"
            typeName == SHORT -> "%L ?: 0"
            typeName == INT -> "%L ?: 0"
            typeName == LONG -> "%L ?: 0"
            typeName == CHAR -> "%L ?: '\\u0000'"
            typeName == FLOAT -> "%L ?: 0.0f"
            typeName == DOUBLE -> "%L ?: 0.0"
            else -> "%L"
        }, name
    )

    fun getProperty(): PropertySpec = buildProperty(name, typeName.nullable) {
        mutable(true)
        delegate(
            "%1T.observable(null)·{·_, _, _·-> %2L = %2L and %3L }",
            Delegates::class.asClassName(),
            DEFAULTS_BITFLAGS_FIELD_NAME,
            (1 shl index).inv()
        )
        if (isMandatory) {
            addAnnotation(DslMandatory::class) {
                useSiteTarget(AnnotationSpec.UseSiteTarget.SET)
            }
        }
    }

    open fun additionalFunctions() = emptyList<FunSpec>()
}