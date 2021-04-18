# AutoDSL for Kotlin (KSP)

Auto-generates [DSL (Domain Specific Language)](https://en.wikipedia.org/wiki/Domain-specific_language)
for your Kotlin projects using annotations.

Inspired by [AutoDsl](https://github.com/juanchosaravia/autodsl), which is implemented for kapt. This project does a similar thing with [Kotlin Symbol
Processing (KSP)](https://github.com/google/ksp).

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
    val friends: List<Person> = emptyList()
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

 **Default values are supported!**
 
## Usage
> Note: This project relies on kotlin 1.5 and KSP for 1.5, which isn't releases yet. You can't use this project unless you compile it and dependencies yourself. This chapter describes how using this project will work once it is released.
 0. [Set up KSP](https://github.com/google/ksp/blob/master/docs/quickstart.md#use-your-own-processor-in-a-project)

 1. Add dependencies:

build.gradle.kts:
```kotlin
dependencies {
    val autoDslVersion = "<latest version>"
    implementation("com.faendir.kotlin.autodsl:annotations:$autoDslVersion")
    ksp("com.faendir.kotlin.autodsl:processor:$autoDslVersion")
}
```
 2. Add `@AutoDsl` to your classes and build your project

 3. Enjoy your new DSL!
