plugins {
    kotlin
    `publish-maven`
    testing
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:${properties["kspVersion"]}")
    implementation(project(":annotations"))
    implementation("com.squareup:kotlinpoet:${properties["kotlinPoetVersion"]}")
    implementation("com.faendir:kotlinbard:${properties["kotlinBardVersion"]}")
    implementation("com.github.cesarferreira:kotlin-pluralizer:${properties["pluralizerVersion"]}")
}
