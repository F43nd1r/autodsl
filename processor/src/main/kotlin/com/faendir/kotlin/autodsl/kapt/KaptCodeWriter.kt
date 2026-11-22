package com.faendir.kotlin.autodsl.kapt

import com.faendir.kotlin.autodsl.CodeWriter
import com.squareup.kotlinpoet.FileSpec
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.tools.Diagnostic

class KaptCodeWriter(
    processingEnv: ProcessingEnvironment,
) : CodeWriter<Type> {
    private val dir: File =
        processingEnv.options["kapt.kotlin.generated"]?.let { File(it) }
            ?: run {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Can't find the target directory for generated Kotlin files. Are you using kapt?",
                )
                throw IllegalStateException()
            }

    override fun emit(
        source: Type,
        fileSpec: FileSpec,
        extra: String,
    ) {
        val packageName = fileSpec.packageName
        val fileName = fileSpec.name
        val packageDir = File(dir, packageName.replace('.', File.separatorChar))
        packageDir.mkdirs()
        val file = File(packageDir, "$fileName.kt")
        file.writer().use {
            fileSpec.writeTo(it)
            if (extra.isNotEmpty()) {
                it.write("\n")
                it.write(extra)
            }
        }
    }
}
