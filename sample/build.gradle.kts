plugins {
    kotlin
    id("com.google.devtools.ksp")
    testing
}

dependencies {
    implementation(project(":annotations"))
    ksp(project(":processor"))
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
