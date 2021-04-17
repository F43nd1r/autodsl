pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenLocal()
    }
}
rootProject.name = "autodsl-ksp"
include("processor")
include("annotations")
include("sample")
