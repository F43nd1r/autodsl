package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.TestFactory

class NestingTest {
    @TestFactory
    fun `basic nesting`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: Entity2)
                @AutoDsl
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
            """
    )

    @TestFactory
    fun `deep nesting`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: Entity2)
                @AutoDsl
                class Entity2(val b: Entity3)
                @AutoDsl
                class Entity3(val c: String)
            """,
        """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity {
                        a {
                            b {
                                c = "Hi"
                            }
                        }
                    }.a.b.c).isEqualTo("Hi")
                }
            """
    )

    @TestFactory
    fun `collection nesting`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity(val a: List<Entity2>, val b: Collection<Entity2>, val c: Set<Entity2>)
                @AutoDsl
                data class Entity2(val d: String)
            """,
        """
                import strikt.api.expectThat
                import strikt.assertions.isA
                import strikt.assertions.containsExactly
                fun test() {
                    expectThat(entity {
                        a {
                            d = "a"
                        }
                        a {
                            d = "a2"
                        }
                        b {
                            d = "b"
                        }
                        c {
                            d = "c"
                        }
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
                @AutoDsl
                class Entity(
                    val friends: List<Entity2>, 
                    val men: List<Entity2>, 
                    val lives: List<Entity2>, 
                    val camelCasedNames: List<Entity2>
                )
                @AutoDsl
                data class Entity2(val a: String)
            """,
        """
                fun test() {
                    entity {
                        friend {
                            a = "a"
                        }
                        man {
                            a = "b"
                        }
                        life {
                            a = "c"
                        }
                        camelCasedName {
                            a = "d"
                        }
                    }
                }
            """
    )

    @TestFactory
    fun `manual singularization`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                import com.faendir.kotlin.autodsl.AutoDslSingular
                @AutoDsl
                class Entity(@AutoDslSingular("clazz") val classes: List<Entity2>)
                @AutoDsl
                data class Entity2(val a: String)
            """,
        """
                fun test() {
                    entity {
                        clazz {
                            a = "a"
                        }
                    }
                }
            """
    )
}