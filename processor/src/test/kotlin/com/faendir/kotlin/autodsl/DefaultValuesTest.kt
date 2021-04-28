package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.Test

class DefaultValuesTest {

    @Test
    fun `default value`() {
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: String = "Hi")
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.com.faendir.kotlin.autodsl.isEqualTo
                fun test() {
                    expectThat(entity {}.a).com.faendir.kotlin.autodsl.isEqualTo("Hi")
                }
            """
        )
    }

    @Test
    fun `primitive default value`() {
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(
                    val a: Boolean = true, 
                    val b: Byte = 1,
                    val c: Short = 2,
                    val d: Int = 3,
                    val e: Long = 4L,
                    val f: Char = 'X',
                    val g: Float = 5.0f,
                    val h: Double = 6.0)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.com.faendir.kotlin.autodsl.isEqualTo
                fun test() {
                    expectThat(entity {}){
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
    fun `default value after required`() {
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: String, val b: String = "Hi")
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.com.faendir.kotlin.autodsl.isEqualTo
                fun test() {
                    expectThat(entity {
                        a = "a"
                    }.b).com.faendir.kotlin.autodsl.isEqualTo("Hi")
                }
            """
        )
    }

    @Test
    fun `default value before required`() {
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: String = "Hi", val b: String)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.com.faendir.kotlin.autodsl.isEqualTo
                fun test() {
                    expectThat(entity {
                        b = "b"
                    }.a).com.faendir.kotlin.autodsl.isEqualTo("Hi")
                }
            """
        )
    }
}