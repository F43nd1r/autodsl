package com.faendir.kotlin.autodsl.kapt

import com.faendir.kotlin.autodsl.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

class KaptProcessor : AbstractProcessor() {
    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        return listOf(AutoDsl::class, AutoDslConstructor::class, AutoDslSingular::class, AutoDslRequired::class, DslMandatory::class)
            .map { it.java.name }.toSet()
    }

    override fun process(elements: MutableSet<out TypeElement>, roundEnvironment: RoundEnvironment): Boolean {
        DslGenerator(
            KotlinVersion.CURRENT,
            KaptLogger(processingEnv.messager),
            KaptCodeWriter(processingEnv),
            KaptSourceInfoResolver(processingEnv, roundEnvironment)
        ).process()
        return false
    }
}