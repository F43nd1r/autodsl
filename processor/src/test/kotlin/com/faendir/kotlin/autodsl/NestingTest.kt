package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.TestFactory

class NestingTest {
    @TestFactory
    fun `basic nesting`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl($SAFETY)
                class Entity(val a: Entity2)
                @AutoDsl($SAFETY)
                class Entity2(val b: String)
            """,
        """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a {
                            b = "Hi"$RETURN_SAFE
                        }$RETURN_SAFE
                    }.a.b).isEqualTo("Hi")
                }
            """
    )

    @TestFactory
    fun `deep nesting`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl($SAFETY)
                class Entity(val a: Entity2)
                @AutoDsl($SAFETY)
                class Entity2(val b: Entity3)
                @AutoDsl($SAFETY)
                class Entity3(val c: String)
            """,
        """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a {
                            b {
                                c = "Hi"$RETURN_SAFE
                            }$RETURN_SAFE
                        }$RETURN_SAFE
                    }.a.b.c).isEqualTo("Hi")
                }
            """
    )

    @TestFactory
    fun `collection nesting`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl($SAFETY)
                class Entity(val a: List<Entity2>, val b: Collection<Entity2>, val c: Set<Entity2>)
                @AutoDsl($SAFETY)
                data class Entity2(val d: String)
            """,
        """
                import strikt.api.expectThat
                import strikt.assertions.isA
                import strikt.assertions.containsExactly
                fun test() {
                    expectThat(entity {
                        a {
                            d = "a"$RETURN_SAFE
                        }
                        a {
                            d = "a2"$RETURN_SAFE
                        }
                        b {
                            d = "b"$RETURN_SAFE
                        }
                        c {
                            d = "c"$RETURN_SAFE
                        }$RETURN_SAFE
                    }){
                        with(Entity::a) {
                            isA<List<Entity2>>()
                            containsExactly(Entity2("a"), Entity2("a2"))
                        }
                        with(Entity::b) {
                            isA<Collection<Entity2>>()
                            containsExactly(Entity2("b"))
                        }
                        with(Entity::c) {
                            isA<Set<Entity2>>()
                            containsExactly(Entity2("c"))
                        }
                    }
                }
            """
    )

    @TestFactory
    fun `automatic singularization`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl($SAFETY)
                class Entity(
                    val friends: List<Entity2>, 
                    val men: List<Entity2>, 
                    val lives: List<Entity2>, 
                    val camelCasedNames: List<Entity2>
                )
                @AutoDsl($SAFETY)
                data class Entity2(val a: String)
            """,
        """
                fun test() {
                    entity {
                        friend {
                            a = "a"$RETURN_SAFE
                        }
                        man {
                            a = "b"$RETURN_SAFE
                        }
                        life {
                            a = "c"$RETURN_SAFE
                        }
                        camelCasedName {
                            a = "d"$RETURN_SAFE
                        }$RETURN_SAFE
                    }
                }
            """
    )

    @TestFactory
    fun `manual singularization`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                import com.faendir.kotlin.autodsl.AutoDslSingular
                @AutoDsl($SAFETY)
                class Entity(@AutoDslSingular("clazz") val classes: List<Entity2>)
                @AutoDsl($SAFETY)
                data class Entity2(val a: String)
            """,
        """
                fun test() {
                    entity {
                        clazz {
                            a = "a"$RETURN_SAFE
                        }$RETURN_SAFE
                    }
                }
            """
    )
}