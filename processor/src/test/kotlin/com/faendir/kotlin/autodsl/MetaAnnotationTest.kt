package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.TestFactory

class MetaAnnotationTest {
    @TestFactory
    fun `meta annotation`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                annotation class MyAutoDsl

                @MyAutoDsl
                class Entity(val a: String)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a = "Hi"
                    }.a).isEqualTo("Hi")
                }
            """,
        )

    @TestFactory
    fun `meta annotation with marker`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl

                annotation class MyDslMarker

                @AutoDsl(MyDslMarker::class)
                annotation class MyAutoDsl

                @MyAutoDsl
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
    fun `meta annotated parameter type`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl

                annotation class MyDslMarker

                @AutoDsl(MyDslMarker::class)
                annotation class MyAutoDsl

                @MyAutoDsl
                class Entity(val a: Entity2)
                @MyAutoDsl
                class Entity2(val b: String)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a {
                            b = "Hi"
                        }
                    }.a.b).isEqualTo("Hi")
                }
            """,
        )
}
