package com.faendir.kotlin.autodsl.sample

import com.faendir.kotlin.autodsl.AutoDsl
import com.faendir.kotlin.autodsl.AutoDslConstructor
import kotlin.math.max

@DslMarker
annotation class MyDsl

@AutoDsl(dslMarker = MyDsl::class)
class Person(
        val name: String = "Hello",
        val age: Int,
        val address: Address?,
        val friends: List<Person> = emptyList()
    )


@AutoDsl(dslMarker = MyDsl::class)
data class Address(
    val street: String,
    val zipCode: Int,
    val location: Location?
)

@AutoDsl(dslMarker = MyDsl::class)
class Location {
    val lat: Double
    val lng: Double

    constructor() {
        lat = 0.0
        lng = 0.0
    }

    // in multiple constructors you can specify which one to use.
    @AutoDslConstructor
    constructor(lat: Double, lng: Double) {
        this.lat = lat
        this.lng = lng
    }
}
