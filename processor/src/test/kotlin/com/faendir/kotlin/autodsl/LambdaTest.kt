package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.TestFactory

class LambdaTest {
    @TestFactory
    fun `simple lambda`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: () -> String)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a = {"Hi"}
                    }.a()).isEqualTo("Hi")
                }
            """,
        )

    @TestFactory
    fun `lambda with parameter`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: (name: String) -> String)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a = { "Hi ${'$'}it" }
                    }.a("F43nd1r")).isEqualTo("Hi F43nd1r")
                }
            """,
        )

    @TestFactory
    fun `lambda with receiver`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: String.() -> String)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a = { "Hi ${'$'}this" }
                    }.a("F43nd1r")).isEqualTo("Hi F43nd1r")
                }
            """,
        )

    @TestFactory
    fun `lambda with default value`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: (name: String) -> String = { "Hi ${'$'}it" })
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {}.a("F43nd1r")).isEqualTo("Hi F43nd1r")
                }
            """,
        )

    @TestFactory
    fun `lambda with receiver and default value`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: String.() -> String = { "Hi ${'$'}this" })
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {}.a("F43nd1r")).isEqualTo("Hi F43nd1r")
                }
            """,
        )
}
