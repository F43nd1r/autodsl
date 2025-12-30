plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        compilerOptions {
            freeCompilerArgs.addAll(listOf("-Xcontext-parameters"))
        }
    }
}