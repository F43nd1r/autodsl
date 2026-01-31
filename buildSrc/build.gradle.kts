plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(libs.kotlin.gradle)
    implementation(libs.dokka.gradle)
    implementation(libs.dokka.core)
    implementation(libs.jgitver)
    implementation(libs.nexusPublish)
    implementation(libs.ksp.gradle)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinter.gradle)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
