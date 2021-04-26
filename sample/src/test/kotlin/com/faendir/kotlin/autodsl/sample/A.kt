package com.faendir.kotlin.autodsl.sample

import com.faendir.kotlin.autodsl.DslInspect
import com.faendir.kotlin.autodsl.DslMandatory

@DslInspect
class PersonBuilder {
    @set:DslMandatory
    var name: String? = null
    @set:DslMandatory
    var age: Int? = null

    fun build() = Person(name!!, age!!)
}
class Person(val name: String, val age: Int)

fun person(initializer: PersonBuilder.() -> Unit) = PersonBuilder().apply(initializer).build()

fun main() {
    val mike = person {
        name = "Mike"
    } //<-"missing properties: age" error here
}