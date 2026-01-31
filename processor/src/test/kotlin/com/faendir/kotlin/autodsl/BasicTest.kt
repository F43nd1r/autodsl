package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.TestFactory

class BasicTest {
    @TestFactory
    fun `basic values`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
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
    fun `primitive values`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(
                    val a: Boolean,
                    val b: Byte,
                    val c: Short,
                    val d: Int,
                    val e: Long,
                    val f: Char,
                    val g: Float,
                    val h: Double)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a = true
                        b = 1
                        c = 2
                        d = 3
                        e = 4L
                        f = 'X'
                        g = 5.0f
                        h = 6.0
                    }){
                        get(Entity::a).isEqualTo(true)
                        get(Entity::b).isEqualTo(1)
                        get(Entity::c).isEqualTo(2)
                        get(Entity::d).isEqualTo(3)
                        get(Entity::e).isEqualTo(4L)
                        get(Entity::f).isEqualTo('X')
                        get(Entity::g).isEqualTo(5.0f)
                        get(Entity::h).isEqualTo(6.0)
                    }
                }
            """,
        )

    @TestFactory
    fun `package name`() =
        compile(
            """
                package com.faendir.test
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: String)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                import com.faendir.test.entity
                fun test() {
                    expectThat(entity {
                        a = "Hi"
                    }.a).isEqualTo("Hi")
                }
            """,
        )

    @TestFactory
    fun `no primary constructor`() =
        compile(
            """
                package com.faendir.test
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity {
                    val a: String
                    constructor(a: String) {
                        this.a = a
                    }
                }
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                import com.faendir.test.entity
                fun test() {
                    expectThat(entity {
                        a = "Hi"
                    }.a).isEqualTo("Hi")
                }
            """,
        )
}
