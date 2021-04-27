dependencies {
    "testImplementation"(kotlin("test-junit5"))
    "testRuntime"("org.junit.jupiter:junit-jupiter-engine:${properties["junitVersion"]}")
    "testImplementation"("com.github.tschuchortdev:kotlin-compile-testing-ksp:${properties["compileTestingVersion"]}")
    "testImplementation"("io.strikt:strikt-core:${properties["striktVersion"]}")
    "testImplementation"(kotlin("scripting-compiler-embeddable"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}