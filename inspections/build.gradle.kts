@Suppress("DSL_SCOPE_VIOLATION") // TODO remove when https://youtrack.jetbrains.com/issue/KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.intellijPlugin)
    java
    kotlin
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(projects.annotations)
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("2022.2.2")
    plugins.set(listOf("org.jetbrains.kotlin", "com.intellij.java"))
    updateSinceUntilBuild.set(false)
}

tasks {
    runIde {
        jvmArgs("--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED", "-Xmx8G")
    }

    patchPluginXml {
        changeNotes.set(
            """
        <b>v2.2</b><br>
        Introduced mandatory groups, of which only one is necessary<br>
        <b>v2.0</b><br>
        Updated for autodsl 2.0<br>
        <b>v1.0</b><br>
        Initial Release<br>
        """
        )
    }

    publishPlugin {
        token.set(project.findProperty("intellijToken") as? String ?: System.getenv("INTELLIJ_TOKEN"))
    }

    register("publish") {
        dependsOn(publishPlugin)
    }
}
