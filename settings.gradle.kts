enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenLocal()
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://s01.oss.sonatype.org/content/repositories/snapshots") }
        mavenLocal()
    }
}
rootProject.name = "autodsl"

rootDir.listFiles()?.forEach {
    if(it.isDirectory && it.name != "buildSrc" && it.list()?.contains("build.gradle.kts") == true) {
        include(it.name)
    }
}
