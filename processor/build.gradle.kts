plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.5.0-RC-dev-experimental-20210417")
    implementation(project(":annotations"))
    implementation("com.squareup:kotlinpoet:1.8.0")
    implementation("com.github.cesarferreira:kotlin-pluralizer:1.0.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
