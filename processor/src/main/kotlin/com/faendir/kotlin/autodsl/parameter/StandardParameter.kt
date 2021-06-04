package com.faendir.kotlin.autodsl.parameter

import com.squareup.kotlinpoet.TypeName

class StandardParameter(typeName: TypeName, name: String, doc: String?, hasDefault: Boolean, requiredGroup: String?, index: Int) :
    Parameter(typeName, name, doc, hasDefault, requiredGroup, index)