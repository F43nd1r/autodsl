package com.faendir.kotlin.autodsl.ksp

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName


fun KSType.asTypeName(): TypeName {
    var name: TypeName = toTypeName()
    if (name is ParameterizedTypeName && name.rawType.canonicalName.matches(Regex("kotlin\\.Function\\d+"))) {
        val arguments = name.typeArguments.toMutableList()
        val receiver = if (annotations.any { it.shortName.asString() == ExtensionFunctionType::class.simpleName }) arguments.removeFirst() else null
        val returnType = arguments.removeLast()
        name = LambdaTypeName.get(receiver = receiver, returnType = returnType, parameters = arguments.toTypedArray())
    }
    return name
}
