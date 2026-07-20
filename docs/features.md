# Features

## Contents

- [Required values and defaults](#required-values-and-defaults)
- [Nested objects](#nested-objects)
- [Collections](#collections)
- [Constructors](#constructors)
- [Reusable annotations and DSL markers](#reusable-annotations-and-dsl-markers)
- [Documentation](#documentation)
- [Limitations](#limitations)

AutoDSL generates a type-safe builder DSL for immutable Kotlin objects. Annotate a class with `@AutoDsl`, compile the project, and use the generated function named after the class.

```kotlin
@AutoDsl
data class Person(
    val name: String,
    val age: Int = 0,
    val address: Address? = null,
)

val person = person {
    name = "Ada"
    age = 36
}
```

The processor generates `PersonBuilder` and `person { ... }` in the same package. Builder properties can be assigned directly or set with fluent functions such as `withName("Ada")`.

## Required values and defaults

Constructor parameters that are non-nullable and have no default value are required. Calling `build()` without assigning one throws an `IllegalStateException`. Install the [Kotlin AutoDSL Inspections](https://plugins.jetbrains.com/plugin/16644-kotlin-autodsl-inspections) IntelliJ plugin to identify missing required values in the editor before runtime.

Nullable parameters may be omitted and become `null`. Parameters with Kotlin default values may also be omitted; the generated builder preserves the constructor default, including defaults that refer to other constructor parameters.

Use `@AutoDslRequired` when a nullable or defaulted parameter must be supplied. Parameters in the same group are alternatives: at least one of them must be assigned.

```kotlin
@AutoDsl
data class Contact(
    @AutoDslRequired("name") val givenName: String? = null,
    @AutoDslRequired("name") val familyName: String? = null,
)

val contact = contact {
    familyName = "Lovelace"
}
```

## Nested objects

When a property type is also annotated with `@AutoDsl`, its builder receives a nested DSL function.

```kotlin
@AutoDsl
data class Address(val city: String)

@AutoDsl
data class Person(val name: String, val address: Address?)

val person = person {
    name = "Ada"
    address {
        city = "London"
    }
}
```

## Collections

`List`, `Set`, `Collection`, and `Iterable` properties receive `with<Property>(vararg values)` helpers. For a collection whose element type is an AutoDSL type, a repeatable singular nested function is also generated.

```kotlin
@AutoDsl
data class Team(val members: List<Person> = emptyList())

val team = team {
    member { name = "Ada" }
    member { name = "Grace" }
}
```

AutoDSL derives the singular name automatically. Override it when necessary with `@AutoDslSingular`.

```kotlin
@AutoDsl
data class Schedule(
    @AutoDslSingular("event") val events: List<Person> = emptyList(),
)
```

## Constructors

AutoDSL uses the primary constructor by default. For a class with multiple constructors, annotate the one the DSL should use with `@AutoDslConstructor`.

```kotlin
@AutoDsl
class Point {
    val x: Int
    val y: Int

    @AutoDslConstructor
    constructor(x: Int, y: Int) {
        this.x = x
        this.y = y
    }
}
```

The selected constructor must be public or internal.

## Reusable annotations and DSL markers

`@AutoDsl` can annotate another annotation class. Apply that annotation to each model class to share configuration. Its `dslMarker` argument is copied to generated builders, so a Kotlin `@DslMarker` can prevent accidental access to an outer DSL receiver.

```kotlin
@DslMarker
annotation class ModelDsl

@AutoDsl(dslMarker = ModelDsl::class)
annotation class GenerateModelDsl

@GenerateModelDsl
data class Address(val city: String)
```

## Documentation

Use `@AutoDslDoc` to attach KDoc to a generated builder or property. This is particularly useful with KSP, which cannot read ordinary source KDoc.

## Limitations

- AutoDSL supports public and internal concrete classes with an accessible constructor. It cannot generate DSLs for interfaces, abstract classes, objects, or enum classes.
- Collection-specific helpers are generated only for `List`, `Set`, `Collection`, and `Iterable`. Maps, arrays, sequences, mutable collection declarations, and custom collections can still be assigned directly.
- A collection nested adder is generated only when its first element type is an AutoDSL class.
- Ordinary source KDoc is not available to KSP. Use `@AutoDslDoc` when generated documentation must work with both KSP and KAPT.
