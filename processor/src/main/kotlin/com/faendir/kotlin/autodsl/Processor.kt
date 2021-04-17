package com.faendir.kotlin.autodsl

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class Processor : SymbolProcessor {
    private lateinit var codeGenerator: CodeGenerator
    private lateinit var logger: KSPLogger

    override fun init(options: Map<String, String>, kotlinVersion: KotlinVersion, codeGenerator: CodeGenerator, logger: KSPLogger) {
        this.codeGenerator = codeGenerator
        this.logger = logger
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val generator = DslGenerator(logger, codeGenerator, resolver)
        for (clazz in resolver.getSymbolsWithAnnotation(AutoDsl::class.java.name).filterIsInstance<KSClassDeclaration>()) {
            when (clazz.classKind) {
                ClassKind.INTERFACE -> logger.error("@AutoDsl can't be applied to $clazz: must not be an interface", clazz)
                ClassKind.ENUM_CLASS -> logger.error("@AutoDsl can't be applied to $clazz: must not be an enum class", clazz)
                ClassKind.ENUM_ENTRY -> logger.error("@AutoDsl can't be applied to $clazz: must not be an enum entry", clazz)
                ClassKind.OBJECT -> logger.error("@AutoDsl can't be applied to $clazz: must not be an object", clazz)
                ClassKind.ANNOTATION_CLASS -> logger.error("@AutoDsl can't be applied to $clazz: must not be an annotation", clazz)
                ClassKind.CLASS -> generator.generate(clazz)
            }
        }
        return emptyList()
    }
}