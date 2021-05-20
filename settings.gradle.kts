pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenLocal()
    }
}
rootProject.name = "autodsl"
include("processor")
include("annotations")
include("sample")
include("inspections")
