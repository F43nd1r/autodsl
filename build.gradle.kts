plugins {
    kotlin("jvm") version "1.5.0-RC" apply false
}

subprojects {
    repositories {
        mavenCentral()
        google()
        maven { setUrl("https://jitpack.io") }
        mavenLocal()
    }
}
