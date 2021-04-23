plugins {
    id("org.jetbrains.intellij") version "0.7.2"
    java
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":annotations"))
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2021.1"
    setPlugins("org.jetbrains.kotlin", "com.intellij.java")
}

tasks.runIde {
    jvmArgs("--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
}

tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes(
        """
        Initial Release
        """
    )
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
