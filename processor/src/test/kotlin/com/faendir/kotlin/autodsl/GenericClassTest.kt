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
}
