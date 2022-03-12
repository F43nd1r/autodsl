package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.TestFactory

class NestedClassTest {

    @TestFactory
    fun `nested dsl class`() = compile(
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
                    expectThat(entity {
                        a = "Hi"
                    }.a).isEqualTo("Hi")
                }
            """
    )

    @TestFactory
    fun `nested class as value`() = compile(
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
            """
    )
}