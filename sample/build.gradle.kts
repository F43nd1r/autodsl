plugins {
    kotlin
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(project(":annotations"))
    ksp(project(":processor"))
    testImplementation(kotlin("test-junit5"))
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.7.1")
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
