package com.faendir.kotlin.autodsl.ksp

import com.faendir.kotlin.autodsl.DslGenerator
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

class KspProcessor(private val kotlinVersion: KotlinVersion, private val out: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> =
        DslGenerator(kotlinVersion, KspLogger(logger), KspCodeWriter(out), KspSourceInfoResolver(resolver)).process()
}