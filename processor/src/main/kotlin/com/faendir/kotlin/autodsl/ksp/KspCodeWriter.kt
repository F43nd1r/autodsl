package com.faendir.kotlin.autodsl.ksp

import com.faendir.kotlin.autodsl.CodeWriter
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec

class KspCodeWriter(
    private val codeGenerator: CodeGenerator,
) : CodeWriter<KSClassDeclaration> {
    override fun emit(
        source: KSClassDeclaration,
        fileSpec: FileSpec,
    ) = codeGenerator.createNewFile(Dependencies(false, source.containingFile!!), fileSpec.packageName, fileSpec.name).writer().use {
        fileSpec.writeTo(it)
    }
}
