package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.Test
import strikt.api.expectThat

class BasicTest {

    @Test
    fun `basic values`() {
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: String)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.com.faendir.kotlin.autodsl.isEqualTo
                fun test() {
                    expectThat(entity {
                        a = "Hi"
                    }.a).com.faendir.kotlin.autodsl.isEqualTo("Hi")
                }
            """
        )
    }

    @Test
    fun `primitive values`() {
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
                import strikt.assertions.com.faendir.kotlin.autodsl.isEqualTo
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
                        get(Entity::a).com.faendir.kotlin.autodsl.isEqualTo(true)
                        get(Entity::b).com.faendir.kotlin.autodsl.isEqualTo(1)
                        get(Entity::c).com.faendir.kotlin.autodsl.isEqualTo(2)
                        get(Entity::d).com.faendir.kotlin.autodsl.isEqualTo(3)
                        get(Entity::e).com.faendir.kotlin.autodsl.isEqualTo(4L)
                        get(Entity::f).com.faendir.kotlin.autodsl.isEqualTo('X')
                        get(Entity::g).com.faendir.kotlin.autodsl.isEqualTo(5.0f)
                        get(Entity::h).com.faendir.kotlin.autodsl.isEqualTo(6.0)
                    }
                }
            """
        )
    }

    @Test
    fun `package name`() {
        compile(
            """
                package com.faendir.test
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: String)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.com.faendir.kotlin.autodsl.isEqualTo
                import com.faendir.test.entity
                fun test() {
                    expectThat(entity {
                        a = "Hi"
                    }.a).com.faendir.kotlin.autodsl.isEqualTo("Hi")
                }
            """
        )
    }
}