package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.TestFactory

class MapTest {
    @TestFactory
    fun `basic map`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
				@AutoDsl
				data class Entity(val a: Map<String, String>) : Map<String, String> by a
			""",
            """
				import strikt.api.expectThat
                import strikt.assertions.isA
                import strikt.assertions.hasEntry
				fun test() {
					expectThat(entity {
						a["a"] = "a"
					}) {
						with(Entity::a) {
							isA<Map<String, String>>()
							hasEntry("a", "a")
						}
					}
				}
			""",
        )

    @TestFactory
    fun `nested map`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
				@AutoDsl
				data class Entity(val a: Map<String, Entity2>) : Map<String, Entity2> by a
				@AutoDsl
				data class Entity2(val b: String)
			""",
            """
				import strikt.api.expectThat
                import strikt.assertions.isA
                import strikt.assertions.hasEntry
				fun test() {
					expectThat(entity {
						a["a"] {
							b = "a"
						}
					}) {
						with(Entity::a) {
							isA<Map<String, Entity2>>()
							hasEntry("a", Entity2("a"))
						}
					}
				}
			""",
        )

    @TestFactory
    fun `generic map`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
				@AutoDsl
				data class Entity<K, V>(val a: Map<K, V>) : Map<K, V> by a
			""",
            """
				import strikt.api.expectThat
                import strikt.assertions.isA
                import strikt.assertions.hasEntry
				fun test() {
					expectThat(entity {
						a["a"] = "a"
					}) {
						with(Entity<String, String>::a) {
							isA<Map<String, String>>()
							hasEntry("a", "a")
						}
					}
				}
			""",
        )
}
