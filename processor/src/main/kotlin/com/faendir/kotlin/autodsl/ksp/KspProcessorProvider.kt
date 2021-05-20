package com.faendir.kotlin.autodsl.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class KspProcessorProvider : SymbolProcessorProvider {
    override fun create(options: Map<String, String>, kotlinVersion: KotlinVersion, codeGenerator: CodeGenerator, logger: KSPLogger): SymbolProcessor {
        return KspProcessor(kotlinVersion, codeGenerator, logger)
    }
}