plugins {
    kotlin
    com.google.devtools.ksp
    testing
}

dependencies {
    implementation(projects.annotations)
    ksp(projects.processor)
}
