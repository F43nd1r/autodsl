[![](https://img.shields.io/maven-central/v/com.faendir.kotlin.autodsl/processor?style=for-the-badge)](https://search.maven.org/search?q=com.faendir.kotlin.autodsl)


# AutoDSL for Kotlin

Auto-generates [DSL (Domain Specific Language)](https://en.wikipedia.org/wiki/Domain-specific_language)
for your Kotlin projects using annotations.

Inspired by [AutoDsl](https://github.com/juanchosaravia/autodsl), which is no longer maintained.

## Documentation

Create expressive, immutable and type-safe DSL **without boilerplate code**:

```kotlin
person {
    name = "Juan"
    age = 34
    address {
        street = "200 Celebration Bv"
        zipCode = 34747
        location {
            lat = 100.0
            lng = 100.0
        }
    }
    friend {
        name = "Arturo"
        age = 28
    }
    friend {
        name = "Tiwa"
        age = 30
    }
}
```

To generate the previous DSL you just need to provide your desired classes with `@AutoDsl`:

```kotlin
@AutoDsl
class Person(
    val name: String,
    val age: Int,
    val address: Address?,
    val friends: List<Person> = emptyList(),
    //in some cases you'll need to provide a valid singular form manually
    @AutoDslSingular("clazz")
    val classes: List<Person> = emptyList()
)


@AutoDsl
data class Address(
    val street: String,
    val zipCode: Int,
    val location: Location?
)

@AutoDsl
class Location {
    val lat: Double
    val lng: Double

    constructor() {
        lat = 0.0
        lng = 0.0
    }

    // with multiple constructors you can specify which one to use.
    @AutoDslConstructor
    constructor(lat: Double, lng: Double) {
        this.lat = lat
        this.lng = lng
    }
}
```
AutoDsl will be generating a builder class and extension function for the annotated class providing this super expressive DSL.

For required parameters like `name` the DSL will throw an exception at runtime. To make it optional just set the property as nullable with the question mark like `address`, or provide a default value like `friends`. The value will be null in case it's not set.

For inspections on required parameters use [this IntelliJ plugin](https://plugins.jetbrains.com/plugin/16644-kotlin-autodsl-inspections).

 **Default values are supported!**
 
## Usage

You can use either ksp or kapt to run this processor. See [here](https://kotlinlang.org/docs/ksp-why-ksp.html#comparison-to-kapt) for a comparison of ksp and kapt.

 1.a) Add dependencies ([Kotlin Symbol Processing (KSP)](https://github.com/google/ksp)):

settings.gradle.kts:
```kotlin
pluginManagement {
    repositories {
       gradlePluginPortal()
       google() //required for ksp
    }
}
```

build.gradle.kts:
```kotlin
plugins {
    id("com.google.devtools.ksp") version "<latest ksp version>" //check https://github.com/google/ksp/releases
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    val autoDslVersion = "<latest version>" //check https://github.com/F43nd1r/autodsl-ksp/releases
    implementation("com.faendir.kotlin.autodsl:annotations:$autoDslVersion")
    ksp("com.faendir.kotlin.autodsl:processor:$autoDslVersion")
}
```

 1.b) Add dependencies ([KAPT](https://kotlinlang.org/docs/kapt.html)):

build.gradle.kts:
```kotlin
plugins {
    kotlin("kapt") version "1.5.0"
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    val autoDslVersion = "<latest version>" //check https://github.com/F43nd1r/autodsl/releases
    implementation("com.faendir.kotlin.autodsl:annotations:$autoDslVersion")
    kapt("com.faendir.kotlin.autodsl:processor:$autoDslVersion")
}
```

 2. Add `@AutoDsl` to your classes and build your project

 3. Enjoy your new DSL!
