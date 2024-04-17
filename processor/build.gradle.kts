plugins {
    kotlin
    `publish-maven`
    testing
}

dependencies {
    implementation(libs.ksp.api)
    implementation(projects.annotations)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.kotlinpoet.metadata)
    implementation(libs.kotlinbard)
    implementation(kotlin("compiler-embeddable"))
}

kotlin.sourceSets.all {
    languageSettings.optIn("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
}

if (JavaVersion.current() >= JavaVersion.VERSION_16) {
    tasks.withType<Test> {
        jvmArgs(
            "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
        )
    }
}
