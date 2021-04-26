plugins {
    kotlin
    `publish-maven`
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:1.5.0-RC-dev-experimental-20210426")
    implementation(project(":annotations"))
    implementation("com.squareup:kotlinpoet:1.8.0")
    implementation("com.github.cesarferreira:kotlin-pluralizer:1.0.0")
}
