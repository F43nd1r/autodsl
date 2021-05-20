plugins {
    id("org.jetbrains.intellij") version "0.7.2"
    java
    kotlin
}

dependencies {
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
        <b>v2.0</b>
        Updated for autodsl 2.0
        <b>v1.0</b>
        Initial Release
        """
    )
}

tasks.withType<org.jetbrains.intellij.tasks.PublishTask> {
    token(project.findProperty("intellijToken") as? String ?: System.getenv("INTELLIJ_TOKEN"))
}

tasks.register("publish") {
    dependsOn("publishPlugin")
}
