package com.faendir.kotlin.autodsl.ksp

import com.faendir.kotlin.autodsl.DslGenerator
import com.faendir.kotlin.autodsl.PsiElementFactory
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

class KspProcessor(
    private val kotlinVersion: KotlinVersion,
    private val out: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private val psiElementFactory = PsiElementFactory()

    override fun process(resolver: Resolver): List<KSAnnotated> =
        DslGenerator(
            kotlinVersion,
            KspLogger(logger),
            KspCodeWriter(out),
            KspSourceInfoResolver(resolver, psiElementFactory),
            psiElementFactory,
        ).process()

    override fun finish() {
        psiElementFactory.dispose()
    }
}
