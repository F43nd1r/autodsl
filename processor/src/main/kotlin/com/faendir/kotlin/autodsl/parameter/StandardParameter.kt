package com.faendir.kotlin.autodsl.parameter

import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName

class StandardParameter(typeName: TypeName, name: String, hasDefault: Boolean, index: Int) : Parameter(typeName, name, hasDefault, index)