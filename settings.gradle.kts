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
include("processor")
include("annotations")
include("sample")
include("inspections")
