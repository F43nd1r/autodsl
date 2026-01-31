package com.faendir.kotlin.autodsl.inspections

import org.junit.Test

class BasicDslInspectionTest :
    DslInspectionTest(
        """
        @DslInspect
        class PersonBuilder {
            @set:DslMandatory("group1")
            var name: String? = null
        }

        fun person(block: PersonBuilder.() -> Unit) {}
        """.trimIndent(),
    ) {
    @Test
    fun `missing property`() {
        configureTest(
            """
            person {
            <error descr="Missing property: name">}</error>
            """.trimIndent(),
        )
        checkHighlighting()
    }

    @Test
    fun `defined property`() {
        configureTest(
            """
            person {
                name = "John"
            }
            """.trimIndent(),
        )
        checkHighlighting()
    }

    @Test
    fun `defined property with this`() {
        configureTest(
            """
            person {
                this.name = "John"
            }
            """.trimIndent(),
        )
        checkHighlighting()
    }

    @Test
    fun `quick fix`() {
        configureTest(
            """
            person {
            <caret>}
            """.trimIndent(),
        )
        checkIntention(
            "name",
            """
            person {
                name = TODO()
            }
            """.trimIndent(),
        )
    }
}
