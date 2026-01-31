package com.faendir.kotlin.autodsl

import com.google.devtools.ksp.symbol.KSNode

interface Logger<S> {
    fun error(
        message: String,
        source: S? = null,
    )
}
