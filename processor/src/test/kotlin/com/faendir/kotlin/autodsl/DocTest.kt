package com.faendir.kotlin.autodsl

import org.junit.jupiter.api.TestFactory

class DocTest {
    @TestFactory
    fun `writes KDoc`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                import com.faendir.kotlin.autodsl.AutoDslDoc

                @AutoDsl
                @AutoDslDoc("Entity comment")
                class Entity(@AutoDslDoc("Parameter comment") val a: String)
            """,
            generates =
                """
                import com.faendir.kotlin.autodsl.DslInspect
                import com.faendir.kotlin.autodsl.DslMandatory
                import kotlin.Boolean
                import kotlin.String
                import kotlin.Unit
                import kotlin.collections.MutableCollection
                import kotlin.collections.MutableMap

                /**
                 * Entity comment
                 */
                @DslInspect
                public class EntityBuilder {
                  /**
                   * Parameter commentParameter comment
                   *
                   * @see Entity.a
                   */
                  @set:DslMandatory(group = "a0")
                  public var a: String? = null

                  /**
                   * Parameter comment
                   *
                   * @see Entity.a
                   */
                  public fun withA(a: String): EntityBuilder {
                    this.a = a
                    return this
                  }

                  public fun build(): Entity {
                    check(a != null) { "a must be assigned." }
                    return Entity(a!!)
                  }
                }

                public inline fun entity(initializer: EntityBuilder.() -> Unit): Entity = EntityBuilder().apply(initializer).build()

                public inline fun MutableCollection<Entity>.add(builder: EntityBuilder.() -> Unit): Boolean = this@add.add(EntityBuilder().apply(builder).build())

                public inline operator fun MutableCollection<Entity>.plusAssign(builder: EntityBuilder.() -> Unit) {
                  this@plusAssign += EntityBuilder().apply(builder).build()
                }

                public inline fun <KEY> MutableMap<KEY, Entity>.put(key: KEY, builder: EntityBuilder.() -> Unit): Entity? = this@put.put(key, EntityBuilder().apply(builder).build())

                public inline operator fun <KEY> MutableMap<KEY, Entity>.`set`(key: KEY, builder: EntityBuilder.() -> Unit) {
                  this@set.put(key, builder)
                }

                """.trimIndent(),
        )

    @TestFactory
    fun `writes multiline KDoc`() =
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                import com.faendir.kotlin.autodsl.AutoDslDoc

                @AutoDsl
                @AutoDslDoc("Entity comment\nWith second line")
                class Entity(@AutoDslDoc("Parameter comment\nWith second line") val a: String)
            """,
            generates =
                """
                import com.faendir.kotlin.autodsl.DslInspect
                import com.faendir.kotlin.autodsl.DslMandatory
                import kotlin.Boolean
                import kotlin.String
                import kotlin.Unit
                import kotlin.collections.MutableCollection
                import kotlin.collections.MutableMap

                /**
                 * Entity comment
                 * With second line
                 */
                @DslInspect
                public class EntityBuilder {
                  /**
                   * Parameter comment
                   * With second lineParameter comment
                   * With second line
                   *
                   * @see Entity.a
                   */
                  @set:DslMandatory(group = "a0")
                  public var a: String? = null

                  /**
                   * Parameter comment
                   * With second line
                   *
                   * @see Entity.a
                   */
                  public fun withA(a: String): EntityBuilder {
                    this.a = a
                    return this
                  }

                  public fun build(): Entity {
                    check(a != null) { "a must be assigned." }
                    return Entity(a!!)
                  }
                }

                public inline fun entity(initializer: EntityBuilder.() -> Unit): Entity = EntityBuilder().apply(initializer).build()

                public inline fun MutableCollection<Entity>.add(builder: EntityBuilder.() -> Unit): Boolean = this@add.add(EntityBuilder().apply(builder).build())

                public inline operator fun MutableCollection<Entity>.plusAssign(builder: EntityBuilder.() -> Unit) {
                  this@plusAssign += EntityBuilder().apply(builder).build()
                }

                public inline fun <KEY> MutableMap<KEY, Entity>.put(key: KEY, builder: EntityBuilder.() -> Unit): Entity? = this@put.put(key, EntityBuilder().apply(builder).build())

                public inline operator fun <KEY> MutableMap<KEY, Entity>.`set`(key: KEY, builder: EntityBuilder.() -> Unit) {
                  this@set.put(key, builder)
                }

                """.trimIndent(),
        )
}
