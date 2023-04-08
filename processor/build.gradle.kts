plugins {
    kotlin
    `publish-maven`
    testing
}

dependencies {
    implementation(libs.ksp.api)
    implementation(projects.annotations)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.kotlinpoet.metadata)
    implementation(libs.kotlinbard)
    implementation(kotlin("compiler-embeddable", "1.8.0"))
}
