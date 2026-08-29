package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.TestFactory

class SealedTest {
    @TestFactory
    fun `sealed class`() =
        compile(
            """
				import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                sealed class Entity<T>(open val value: T)
				{
					class StringEntity(override val value: String) : Entity<String>(value)
					class IntEntity(override val value: Int) : Entity<Int>(value)
				}
		""",
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity<String> {
                        string {
							value = "Hi"
						}
                    }.value).isEqualTo("Hi")
                }
			""",
        )

    @TestFactory
    fun `sealed class with collection`() =
        compile(
            """
				import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                sealed class Entity<T>(open val value: List<T>)
				{
					class StringEntity(override val value: List<String>) : Entity<String>(value)
					class IntEntity(override val value: List<Int>) : Entity<Int>(value)
				}
		""",
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity<String> {
                        string {
							value += "Hi"
						}
                    }.value).isEqualTo(listOf("Hi"))
                }
			""",
        )

    @TestFactory
    fun `sealed class with nested`() =
        compile(
            """
				import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                sealed class Node<T>(open val value: T)
				{
					class StringNode(override val value: String) : Node<String>(value)
					class IntNode(override val value: Int) : Node<Int>(value)
				}
				@AutoDsl
				class Entity<T>(val node: Node<T>)
		""",
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity<String> {
						string node {
							value = "Hi"
						}
                    }.node.value).isEqualTo("Hi")
                }
			""",
        )

    @TestFactory
    fun `sealed class with nested collection`() =
        compile(
            """
				import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                sealed class Node<T>(open val value: T)
				{
					class StringNode(override val value: String) : Node<String>(value)
					class IntNode(override val value: Int) : Node<Int>(value)
				}
				@AutoDsl
				class Entity<T>(val nodes: List<Node<T>>)
		""",
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
					expectThat(entity<String> {
					    string node {
					        value = "Hi"
					    }
					}.nodes.first().value).isEqualTo("Hi")
                }
			""",
        )

    @TestFactory
    fun `sealed interface`() =
        compile(
            """
				import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                sealed interface Entity<T>
				{
					val value: T
					class StringEntity(override val value: String) : Entity<String>
					class IntEntity(override val value: Int) : Entity<Int>
				}
		""",
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity<String> {
                        string {
							value = "Hi"
						}
                    }.value).isEqualTo("Hi")
                }
			""",
        )

    @TestFactory
    fun `building without selecting a subclass throws`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                sealed class Entity<T>(open val value: T)
                {
                    class StringEntity(override val value: String) : Entity<String>(value)
                    class IntEntity(override val value: Int) : Entity<Int>(value)
                }
            """,
            """
                import strikt.api.expectThrows
                fun test() {
                    expectThrows<IllegalArgumentException> {
                        entity<String> { }
                    }
                }
            """,
        )

    @TestFactory
    fun `mandatory field inside a selected subclass is still validated`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                sealed class Entity<T>(open val value: T)
                {
                    class StringEntity(override val value: String) : Entity<String>(value)
                    class IntEntity(override val value: Int) : Entity<Int>(value)
                }
            """,
            """
                import strikt.api.expectThrows
                fun test() {
                    expectThrows<IllegalStateException> {
                        entity<String> {
                            string { }
                        }
                    }
                }
            """,
        )

    @TestFactory
    fun `non-generic sealed class`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                sealed class Shape
                {
                    class Circle(val radius: Double) : Shape()
                    class Square(val side: Double) : Shape()
                }
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    val result = shape {
                        circle {
                            radius = 2.0
                        }
                    }
                    expectThat((result as Shape.Circle).radius).isEqualTo(2.0)
                }
            """,
        )

    @TestFactory
    fun `sealed class with three subclasses`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                sealed class Entity<T>(open val value: T)
                {
                    class StringEntity(override val value: String) : Entity<String>(value)
                    class IntEntity(override val value: Int) : Entity<Int>(value)
                    class BooleanEntity(override val value: Boolean) : Entity<Boolean>(value)
                }
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    expectThat(entity<Boolean> {
                        boolean {
                            value = true
                        }
                    }.value).isEqualTo(true)
                }
            """,
        )

    @TestFactory
    fun `subclass name unrelated to the parent name`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                sealed class Shape
                {
                    class Round(val radius: Double) : Shape()
                }
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    val result = shape {
                        round {
                            radius = 1.0
                        }
                    }
                    expectThat((result as Shape.Round).radius).isEqualTo(1.0)
                }
            """,
        )

    @TestFactory
    fun `two collections of the same nested sealed type`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                sealed class Node<T>(open val value: T)
                {
                    class StringNode(override val value: String) : Node<String>(value)
                    class IntNode(override val value: Int) : Node<Int>(value)
                }
                @AutoDsl
                class Entity<T>(val primaryNodes: List<Node<T>>, val secondaryNodes: List<Node<T>>)
            """,
            """
                import strikt.api.expectThat
                import strikt.assertions.isEqualTo
                fun test() {
                    val result = entity<String> {
                        string primaryNode {
                            value = "A"
                        }
                        string secondaryNode {
                            value = "B"
                        }
                    }
                    expectThat(result.primaryNodes.first().value).isEqualTo("A")
                    expectThat(result.secondaryNodes.first().value).isEqualTo("B")
                }
            """,
        )
}
