package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.TestFactory

class NestedClassTest {
    @TestFactory
    fun `nested dsl class`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                class Outer {
                    @AutoDsl
                    class Entity(val a: String)
                }
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(outerEntity {
                        a = "Hi"
                    }.a).isEqualTo("Hi")
                }
            """,
        )

    @TestFactory
    fun `nested class as value`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: A) {
                    data class A(val a: String)
                }
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a = Entity.A("Hi")
                    }){
                        get(Entity::a).isEqualTo(Entity.A("Hi"))
                    }
                }
            """,
        )

    @TestFactory
    fun `same-named nested classes in different outer classes`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Outer1(val contact: Contact) {
                    @AutoDsl
                    class Contact(val name: String, val email: String)
                }
                @AutoDsl
                class Outer2(val contact: Contact) {
                    @AutoDsl
                    class Contact(val name: String, val phone: String)
                }
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    val o1 = outer1 {
                        contact {
                            name = "Alice"
                            email = "alice@example.com"
                        }
                    }
                    expectThat(o1.contact.name).isEqualTo("Alice")
                    expectThat(o1.contact.email).isEqualTo("alice@example.com")
                    val o2 = outer2 {
                        contact {
                            name = "Bob"
                            phone = "123"
                        }
                    }
                    expectThat(o2.contact.name).isEqualTo("Bob")
                    expectThat(o2.contact.phone).isEqualTo("123")
                }
            """,
        )
}
