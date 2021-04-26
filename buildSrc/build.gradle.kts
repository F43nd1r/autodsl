plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    val kotlinVersion: String by project
    implementation(kotlin("gradle-plugin:$kotlinVersion"))
    val dokkaVersion: String by project
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-core:$dokkaVersion")
    val jgitverVersion: String by project
    implementation("gradle.plugin.fr.brouillard.oss.gradle:gradle-jgitver-plugin:$jgitverVersion")
    val nexusPublishVersion: String by project
    implementation("io.github.gradle-nexus:publish-plugin:$nexusPublishVersion")
    val kspVersion: String by project
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:$kspVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
