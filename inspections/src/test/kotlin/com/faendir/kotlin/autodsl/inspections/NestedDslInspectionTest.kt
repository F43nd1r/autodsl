package com.faendir.kotlin.autodsl.inspections

import org.junit.Test

class NestedDslInspectionTest :
    DslInspectionTest(
        """
        @DslInspect
        class AddressBuilder {
            @set:DslMandatory(group = "street0")
            var street: String? = null

            fun build(): Address = TODO()
        }

        @DslInspect
        class PersonBuilder {
            @set:DslMandatory("group1")
            var address: Address? = null

            inline fun address(initializer: AddressBuilder.() -> Unit): Address {
                val result = AddressBuilder().apply(initializer).build()
                address = result
                return result
           }
        }

        fun person(block: PersonBuilder.() -> Unit) {}
        """.trimIndent(),
    ) {
    @Test
    fun `missing property`() {
        configureTest(
            """
            person {
            <error descr="Missing property: address">}</error>
            """.trimIndent(),
        )
        checkHighlighting()
    }

    @Test
    fun `defined nested property`() {
        configureTest(
            """
            person {
                address {
                    street = "Main St"
                }
            }
            """.trimIndent(),
        )
        checkHighlighting()
    }

    @Test
    fun `defined nested property with this`() {
        configureTest(
            """
            person {
                this.address {
                    street = "Main St"
                }
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
            "address",
            """
            person {
                address { TODO() }
            }
            """.trimIndent(),
        )
    }
}
