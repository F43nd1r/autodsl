package com.faendir.kotlin.autodsl.sample

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Test {
    @Test
    fun test() {
        val person = person {
            name = "Max Mustermann"
            age = 40
            friend {
                age = 10
            }
        }
        assertEquals("Hello", person.friends.first().name)
    }
}