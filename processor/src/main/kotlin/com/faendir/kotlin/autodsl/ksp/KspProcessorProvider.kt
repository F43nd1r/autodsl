package com.faendir.kotlin.autodsl.ksp

import com.google.devtools.ksp.processing.*

class KspProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return KspProcessor(environment.kotlinVersion, environment.codeGenerator, environment.logger)
    }
}