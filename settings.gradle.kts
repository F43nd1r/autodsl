enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenLocal()
    }
}
rootProject.name = "autodsl"

rootDir.listFiles()?.forEach {
    if(it.isDirectory && it.name != "buildSrc" && it.list()?.contains("build.gradle.kts") == true) {
        include(it.name)
    }
}
