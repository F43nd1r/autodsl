package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.TestFactory

class DefaultValuesTest {

    @TestFactory
    fun `default value`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl($SAFETY)
                class Entity(val a: String = "Hi")
            """,
        """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {$RETURN_SAFE}.a).isEqualTo("Hi")
                }
            """
    )

    @TestFactory
    fun `primitive default value`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl($SAFETY)
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
                    expectThat(entity {$RETURN_SAFE}){
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
    fun `nullable primitive values with default value present`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl($SAFETY)
                class Entity(
                    val a: Boolean?, 
                    val b: Byte?,
                    val c: Short?,
                    val d: Int?,
                    val e: Long?,
                    val f: Char?,
                    val g: Float?,
                    val h: Double?,
                    val i: String = "i")
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
                        h = 6.0$RETURN_SAFE
                    }){
                        get(Entity::a).isEqualTo(true)
                        get(Entity::b).isEqualTo(1)
                        get(Entity::c).isEqualTo(2)
                        get(Entity::d).isEqualTo(3)
                        get(Entity::e).isEqualTo(4L)
                        get(Entity::f).isEqualTo('X')
                        get(Entity::g).isEqualTo(5.0f)
                        get(Entity::h).isEqualTo(6.0)
                        get(Entity::i).isEqualTo("i")
                    }
                }
            """
    )

    @TestFactory
    fun `default value after required`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl($SAFETY)
                class Entity(val a: String, val b: String = "Hi")
            """,
        """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a = "a"$RETURN_SAFE
                    }.b).isEqualTo("Hi")
                }
            """
    )

    @TestFactory
    fun `default value before required`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl($SAFETY)
                class Entity(val a: String = "Hi", val b: String)
            """,
        """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        b = "b"$RETURN_SAFE
                    }.a).isEqualTo("Hi")
                }
            """
    )

    @TestFactory
    fun `default values with over 32 parameters`() = compile(
    """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl($SAFETY)
                class Entity(
                    val a1: String = "a1",
                    val a2: String = "a2",
                    val a3: String = "a3",
                    val a4: String = "a4",
                    val a5: String = "a5",
                    val a6: String = "a6",
                    val a7: String = "a7",
                    val a8: String = "a8",
                    val a9: String = "a9",
                    val a10: String = "a10",
                    val a11: String = "a11",
                    val a12: String = "a12",
                    val a13: String = "a13",
                    val a14: String = "a14",
                    val a15: String = "a15",
                    val a16: String = "a16",
                    val a17: String = "a17",
                    val a18: String = "a18",
                    val a19: String = "a19",
                    val a20: String = "a20",
                    val a21: String = "a21",
                    val a22: String = "a22",
                    val a23: String = "a23",
                    val a24: String = "a24",
                    val a25: String = "a25",
                    val a26: String = "a26",
                    val a27: String = "a27",
                    val a28: String = "a28",
                    val a29: String = "a29",
                    val a30: String = "a30",
                    val a31: String = "a31",
                    val a32: String = "a32",
                    val a33: String = "a33",
                )
            """,
    """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    val entity = entity {$RETURN_SAFE}
                    expectThat(entity.a1).isEqualTo("a1")
                    expectThat(entity.a2).isEqualTo("a2")
                    expectThat(entity.a3).isEqualTo("a3")
                    expectThat(entity.a4).isEqualTo("a4")
                    expectThat(entity.a5).isEqualTo("a5")
                    expectThat(entity.a6).isEqualTo("a6")
                    expectThat(entity.a7).isEqualTo("a7")
                    expectThat(entity.a8).isEqualTo("a8")
                    expectThat(entity.a9).isEqualTo("a9")
                    expectThat(entity.a10).isEqualTo("a10")
                    expectThat(entity.a11).isEqualTo("a11")
                    expectThat(entity.a12).isEqualTo("a12")
                    expectThat(entity.a13).isEqualTo("a13")
                    expectThat(entity.a14).isEqualTo("a14")
                    expectThat(entity.a15).isEqualTo("a15")
                    expectThat(entity.a16).isEqualTo("a16")
                    expectThat(entity.a17).isEqualTo("a17")
                    expectThat(entity.a18).isEqualTo("a18")
                    expectThat(entity.a19).isEqualTo("a19")
                    expectThat(entity.a20).isEqualTo("a20")
                    expectThat(entity.a21).isEqualTo("a21")
                    expectThat(entity.a22).isEqualTo("a22")
                    expectThat(entity.a23).isEqualTo("a23")
                    expectThat(entity.a24).isEqualTo("a24")
                    expectThat(entity.a25).isEqualTo("a25")
                    expectThat(entity.a26).isEqualTo("a26")
                    expectThat(entity.a27).isEqualTo("a27")
                    expectThat(entity.a28).isEqualTo("a28")
                    expectThat(entity.a29).isEqualTo("a29")
                    expectThat(entity.a30).isEqualTo("a30")
                    expectThat(entity.a31).isEqualTo("a31")
                    expectThat(entity.a32).isEqualTo("a32")
                    expectThat(entity.a33).isEqualTo("a33")
                }
            """
    )
}