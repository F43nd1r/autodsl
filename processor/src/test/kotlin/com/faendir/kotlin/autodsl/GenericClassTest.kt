package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.TestFactory

class GenericClassTest {
    @TestFactory
    fun `basic type parameter`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity<T>(val a: T)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity<String> {
                        a = "Hi"
                    }.a).isEqualTo("Hi")
                }
            """,
        )

    @TestFactory
    fun `collection type parameter`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity<T>(val a: List<T>)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity<String> {
                        a = listOf("Hi")
                    }.a).isEqualTo(listOf("Hi"))
                }
            """,
        )

    @TestFactory
    fun `nullable type parameter`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity<T>(val a: T?)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity<String> {
                        a = null
                    }.a).isEqualTo(null)
                }
            """,
        )

    @TestFactory
    fun `dependent type parameters`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity<T, L: List<T>>(val a: L)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity<String, List<String>> {
                        a = listOf("Hi")
                    }.a).isEqualTo(listOf("Hi"))
                }
            """,
        )

    @TestFactory
    fun `bounded type parameter`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity<T : Comparable<T>>(val a: T)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity<String> {
                        a = "Hi"
                    }.a).isEqualTo("Hi")
                }
            """,
        )

    @TestFactory
    fun `multiple type parameters`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity<T, U>(val a: T, val b: U)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    val entity = entity<String, Int> {
                        a = "Hi"
                        b = 1
                    }
                    expectThat(entity.a).isEqualTo("Hi")
                    expectThat(entity.b).isEqualTo(1)
                }
            """,
        )

    @TestFactory
    fun `nested dsl with type parameter`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity<T>(val a: T)
                @AutoDsl
                class Root(val entity: Entity<String>)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(root {
                        entity {
                            a = "Hi"
                        }
                    }.entity.a).isEqualTo("Hi")
                }
            """,
        )

    @TestFactory
    fun `nested dsl with multiple type parameters`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity<T, U>(val a: T, val b: U)
                @AutoDsl
                class Root(val entity: Entity<String, Int>)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(root {
                        entity {
                            a = "Hi"
                            b = 1
                        }
                    }.entity.a).isEqualTo("Hi")
                }
            """,
        )

    @TestFactory
    fun `nested dsl with star projection`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity<T>(val a: T)
                @AutoDsl
                class Root(val entity: Entity<*>)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(root {
                        entity<String> {
                            a = "Hi"
                        }
                    }.entity.a).isEqualTo("Hi")
                }
            """,
        )

    @TestFactory
    fun `nested dsl with out projection`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity<T>(val a: T)
                @AutoDsl
                class Root(val entity: Entity<out String>)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(root {
                        entity {
                            a = "Hi"
                        }
                    }.entity.a).isEqualTo("Hi")
                }
            """,
        )
}
