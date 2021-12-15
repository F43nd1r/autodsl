plugins {
    kotlin
    `publish-maven`
    testing
}

dependencies {
    implementation(libs.ksp.api)
    implementation(projects.annotations)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.kotlinpoet.metadataSpecs)
    implementation(libs.kotlinpoet.classinspectorElements)
    implementation(libs.kotlinbard)
    implementation(libs.kotlinPluralizer)
    implementation(kotlin("compiler-embeddable"))
}
