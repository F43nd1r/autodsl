plugins {
    kotlin("jvm")
    id("org.jmailen.kotlinter")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlinter {
}
