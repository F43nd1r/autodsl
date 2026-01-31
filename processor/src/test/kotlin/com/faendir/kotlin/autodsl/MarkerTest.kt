package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.TestFactory

class MarkerTest {
    @TestFactory
    fun `marker in same module`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl

                annotation class MyDslMarker

                @AutoDsl(MyDslMarker::class)
                class Entity(val a: String)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                import strikt.assertions.isNotNull
                fun test() {
                    expectThat(EntityBuilder::class.java).get { getAnnotation(MyDslMarker::class.java) }.isNotNull()
                    expectThat(entity {
                        a = "Hi"
                    }.a).isEqualTo("Hi")
                }
            """,
        )

    @TestFactory
    fun `marker from other module`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                import org.junit.jupiter.api.Disabled

                @AutoDsl(Disabled::class)
                class Entity(val a: String)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                import strikt.assertions.isNotNull
                import org.junit.jupiter.api.Disabled

                fun test() {
                    expectThat(EntityBuilder::class.java).get { getAnnotation(Disabled::class.java) }.isNotNull()
                    expectThat(entity {
                        a = "Hi"
                    }.a).isEqualTo("Hi")
                }
            """,
        )
}
