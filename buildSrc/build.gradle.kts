plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(libs.kotlin.gradle)
    implementation(libs.dokka.gradle)
    implementation(libs.dokka.core)
    implementation(libs.jgitver)
    implementation(libs.nexusPublish)
    implementation(libs.ksp.gradle)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
