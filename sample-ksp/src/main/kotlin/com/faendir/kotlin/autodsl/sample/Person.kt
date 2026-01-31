package com.faendir.kotlin.autodsl.sample

import com.faendir.kotlin.autodsl.AutoDsl
import com.faendir.kotlin.autodsl.AutoDslConstructor
import com.faendir.kotlin.autodsl.AutoDslRequired

@DslMarker
annotation class MyDsl

@AutoDsl(dslMarker = MyDsl::class)
annotation class MetaAutoDsl

@AutoDsl(dslMarker = MyDsl::class)
class Person(
    val name: String = "Max",
    val age: Int,
    val address: Address?,
    val friends: List<Person> = emptyList(),
    @AutoDslRequired("n")
    val givenName: String? = null,
    @AutoDslRequired("n")
    val streetName: String? = null,
)

@MetaAutoDsl
data class Address(
    val street: String,
    val zipCode: Int,
    val location: Location?,
)

@AutoDsl(dslMarker = MyDsl::class)
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
