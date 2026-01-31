package com.faendir.kotlin.autodsl.ksp

import com.faendir.kotlin.autodsl.Logger
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration

class KspLogger(
    private val delegate: KSPLogger,
) : Logger<KSClassDeclaration> {
    override fun error(
        message: String,
        source: KSClassDeclaration?,
    ) = delegate.error(message, source)
}
