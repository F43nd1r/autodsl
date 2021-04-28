package com.faendir.kotlin.autodsl.parameter

import com.google.devtools.ksp.symbol.KSValueParameter

class StandardParameter(parameter: KSValueParameter, index: Int) : Parameter(parameter, index)