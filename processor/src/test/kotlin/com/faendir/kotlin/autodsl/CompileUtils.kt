package com.faendir.kotlin.autodsl

import com.faendir.kotlin.autodsl.kapt.KaptProcessor
import com.faendir.kotlin.autodsl.ksp.KspProcessorProvider
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile.Companion.fromPath
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import com.tschuchort.compiletesting.configureKsp
import java.io.File
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DynamicTest
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo

internal val JvmCompilationResult.workingDir: File
    get() = outputDirectory.parentFile!!

val JvmCompilationResult.kspGeneratedSources: List<File>
    get() = workingDir.resolve("ksp/sources/kotlin").collectSourceFiles()

private fun File.collectSourceFiles(): List<File> = walkTopDown().filter { it.isFile }.toList()

fun compile(
    @Language("kotlin") source: String,
    @Language("kotlin") eval: String = "fun test() { }",
    @Language("kotlin") generates: String? = null,
    expect: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
    compare: Boolean = true,
): List<DynamicTest> {
    val compileKsp by lazy { compileKsp(source, eval, expect) }
    val compileKapt by lazy { compileKapt(source, eval, expect) }

    return listOfNotNull(
        DynamicTest.dynamicTest("ksp") { compileKsp },
        DynamicTest.dynamicTest("kapt") { compileKapt },
        if (compare) {
            DynamicTest.dynamicTest("compare") {
                expectThat(compileKsp.map { it.readText() }).containsExactlyInAnyOrder(compileKapt.map { it.readText() })
            }
        } else {
            null
        },
        generates?.let { DynamicTest.dynamicTest("generates") { expectThat(compileKsp.map { it.readText() }).contains(generates) } },
    )
}

fun compileKsp(
    @Language("kotlin") source: String,
    @Language("kotlin") eval: String,
    expect: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
): List<File> {
    val compilation =
        KotlinCompilation().apply {
            inheritClassPath = true
            jvmTarget = "17"
            sources = listOf(kotlin("Source.kt", source))
            configureKsp {
                symbolProcessorProviders.add(KspProcessorProvider())
            }
        }
    val pass1 = compilation.compile()
    expectThat(pass1).get(JvmCompilationResult::exitCode).isEqualTo(expect)
    val pass2 =
        KotlinCompilation()
            .apply {
                inheritClassPath = true
                jvmTarget = "17"
                sources = compilation.sources + pass1.kspGeneratedSources.map { fromPath(it) } + kotlin("Eval.kt", eval)
            }.compile()
    expectThat(pass2).get(JvmCompilationResult::exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    pass2.callEval()
    return pass1.kspGeneratedSources
}

fun compileKapt(
    @Language("kotlin") source: String,
    @Language("kotlin") eval: String,
    expect: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
): List<File> {
    val result =
        KotlinCompilation()
            .apply {
                inheritClassPath = true
                jvmTarget = "17"
                sources = listOf(kotlin("Source.kt", source), kotlin("Eval.kt", eval))
                useKapt4 = true
                annotationProcessors = listOf(KaptProcessor())
            }.compile()
    expectThat(result).get(JvmCompilationResult::exitCode).isEqualTo(expect)
    if (expect == KotlinCompilation.ExitCode.OK) {
        result.callEval()
    }
    return result.generatedFiles.filter { it.extension == "kt" }
}

private fun JvmCompilationResult.callEval() =
    classLoader
        .loadClass("EvalKt")
        .declaredMethods
        .first {
            !it.name.contains('$') // skip generated functions
        }.run {
            isAccessible = true
            invoke(null)
        }
