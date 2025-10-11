plugins {
    jacoco
    id("kotlin")
}

if(canUseLibs()) {
    dependencies {
        testImplementation(kotlin("test-junit5"))
        testImplementation(libs.junit)
        testImplementation(libs.kotlin.compileTesting)
        testImplementation(libs.strikt)
        testImplementation(kotlin("scripting-compiler-embeddable"))
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy("jacocoTestReport")
}

jacoco {
    toolVersion = "0.8.14"
}