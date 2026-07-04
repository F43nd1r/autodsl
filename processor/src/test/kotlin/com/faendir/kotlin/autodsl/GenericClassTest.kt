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

    @TestFactory
    fun `nested dsl with multiple type parameters sets both properties`() =
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
                    val r = root {
                        entity {
                            a = "Hi"
                            b = 1
                        }
                    }
                    expectThat(r.entity.a).isEqualTo("Hi")
                    expectThat(r.entity.b).isEqualTo(1)
                }
            """,
        )

    @TestFactory
    fun `multiple bounds via where clause`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity<T>(val a: T) where T : Comparable<T>, T : CharSequence
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
    fun `type parameter unused by any constructor parameter`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity<T>(val a: String)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity<Int> {
                        a = "Hi"
                    }.a).isEqualTo("Hi")
                }
            """,
        )

    @TestFactory
    fun `generic parameter combined with a default value`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity<T>(val a: T?, val b: String = "b")
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    val e = entity<String> {
                        a = "Hi"
                    }
                    expectThat(e.a).isEqualTo("Hi")
                    expectThat(e.b).isEqualTo("b")
                }
            """,
        )

    @TestFactory
    fun `covariant type parameter`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity<out T>(val a: T)
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
}
