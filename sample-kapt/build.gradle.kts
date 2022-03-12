plugins {
    kotlin
    org.jetbrains.kotlin.kapt
    testing
}

dependencies {
    implementation(projects.annotations)
    kapt(projects.processor)
}
