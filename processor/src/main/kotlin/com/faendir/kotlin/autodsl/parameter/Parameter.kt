package com.faendir.kotlin.autodsl.parameter

import com.faendir.kotlin.autodsl.DEFAULTS_BITFLAGS_FIELD_NAME
import com.faendir.kotlin.autodsl.DslMandatory
import com.faendir.kotlin.autodsl.nonnull
import com.faendir.kotlin.autodsl.toRawType
import com.squareup.kotlinpoet.*
import io.github.enjoydambience.kotlinbard.addAnnotation
import io.github.enjoydambience.kotlinbard.buildFunction
import io.github.enjoydambience.kotlinbard.buildProperty
import io.github.enjoydambience.kotlinbard.nullable
import kotlin.properties.Delegates

abstract class Parameter(
    val typeName: TypeName,
    val name: String,
    val doc: String?,
    val hasDefault: Boolean,
    val requiredGroup: String?,
    private val index: Int
) {
    val isMandatory = requiredGroup != null || !hasDefault && !typeName.isNullable

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

    fun getProperty(referencedType: TypeName): PropertySpec = buildProperty(name, typeName.nullable) {
        mutable(true)
        delegate(
            "%1T.observable(null)·{·_, _, _·-> %2L·= %2L and %3L }",
            Delegates::class.asClassName(),
            DEFAULTS_BITFLAGS_FIELD_NAME,
            (1 shl index).inv()
        )
        if (isMandatory) {
            addAnnotation(DslMandatory::class) {
                useSiteTarget(AnnotationSpec.UseSiteTarget.SET)
                addMember("group = %S", requiredGroup ?: name + index)
            }
        }
        doc?.let { addKdoc(it) }
        addKdoc("@see %T.%L", referencedType, name)
    }

    fun getBuilderFunction(referencedType: TypeName, builderType: TypeName) = buildFunction("with${name.replaceFirstChar { it.uppercase() }}") {
        returns(builderType)
        addParameter(name, typeName)
        addStatement("return apply·{ this.%1L·= %1L }", name)
        doc?.let { addKdoc(it) }
        addKdoc("@see %T.%L", referencedType, name)
    }

    open fun additionalFunctions(referencedType: TypeName, builderType: TypeName) = emptyList<FunSpec>()
}