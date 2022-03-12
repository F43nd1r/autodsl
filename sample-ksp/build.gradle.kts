plugins {
    kotlin
    com.google.devtools.ksp
    testing
}

dependencies {
    implementation(projects.annotations)
    ksp(projects.processor)
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
