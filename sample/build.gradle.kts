plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "1.5.0-RC-dev-experimental-20210417"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":annotations"))
    ksp(project(":processor"))
    testImplementation(kotlin("test-junit5"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

sourceSets {
    main {
        java {
            srcDir(file("$buildDir/generated/ksp/main/kotlin"))
        }
    }
    test {
        java {
            srcDir(file("$buildDir/generated/ksp/test/kotlin"))
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
