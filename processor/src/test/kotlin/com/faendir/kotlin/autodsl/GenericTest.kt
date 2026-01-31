package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.TestFactory

class GenericTest {
    @TestFactory
    fun `star projection`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: List<*>)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a = listOf("Hi")
                    }.a).isEqualTo(listOf("Hi"))
                }
            """,
        )

    @TestFactory
    fun invariant() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: List<Any>)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a = listOf("Hi")
                    }.a).isEqualTo(listOf("Hi"))
                }
            """,
        )

    @TestFactory
    fun `out projection`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: List<out Any>)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a = listOf("Hi")
                    }.a).isEqualTo(listOf("Hi"))
                }
            """,
        )

    @TestFactory
    fun `in projection`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: Class<in String>)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a = String::class.java
                    }.a).isEqualTo(String::class.java)
                }
            """,
        )

    @TestFactory
    fun `java type`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: List<Class<*>>)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a = listOf(String::class.java)
                    }.a).isEqualTo(listOf(String::class.java))
                }
            """,
        )
}
