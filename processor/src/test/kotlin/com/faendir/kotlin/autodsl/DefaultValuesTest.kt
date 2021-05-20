package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.TestFactory

class DefaultValuesTest {

    @TestFactory
    fun `default value`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: String = "Hi")
            """,
        """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {}.a).isEqualTo("Hi")
                }
            """
    )

    @TestFactory
    fun `primitive default value`() = compile(
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
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {}){
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
            """
    )

    @TestFactory
    fun `default value after required`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: String, val b: String = "Hi")
            """,
        """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a = "a"
                    }.b).isEqualTo("Hi")
                }
            """
    )

    @TestFactory
    fun `default value before required`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: String = "Hi", val b: String)
            """,
        """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        b = "b"
                    }.a).isEqualTo("Hi")
                }
            """
    )
}