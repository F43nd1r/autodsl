import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    alias(libs.plugins.intellijPlugin)
    java
    kotlin
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation(projects.annotations)

    // See https://github.com/JetBrains/gradle-intellij-plugin/
    intellijPlatform {
        intellijIdea(libs.versions.intellijIdea)
        bundledPlugins(listOf("org.jetbrains.kotlin", "com.intellij.java"))
        testFramework(TestFrameworkType.Bundled)
    }

    testImplementation(libs.junit.vintage)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            untilBuild = provider { null }
        }
    }
    publishing {
        token = project.findProperty("intellijToken") as? String ?: System.getenv("INTELLIJ_TOKEN")
    }
}

tasks {
    buildSearchableOptions {
        enabled = false
    }

    runIde {
        jvmArgs("--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED", "-Xmx8G")
    }

    patchPluginXml {
        changeNotes.set(
            """
        <b>v3.0</b><br>
        Kotlin K2 support<br>
        <b>v2.2</b><br>
        Introduced mandatory groups, of which only one is necessary<br>
        <b>v2.0</b><br>
        Updated for autodsl 2.0<br>
        <b>v1.0</b><br>
        Initial Release<br>
        """,
        )
    }

    register("publish") {
        dependsOn(publishPlugin)
    }
}
