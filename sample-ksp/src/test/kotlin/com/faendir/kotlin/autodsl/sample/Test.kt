package com.faendir.kotlin.autodsl.sample

import org.junit.jupiter.api.Test

class Test {
    @Test
    fun test() {
        person {
            name = "Juan"
            givenName = "Juanno"
            age = 34
            address {
                street = "200 Celebration Bv"
                zipCode = 34747
                location {
                    lat = 100.0
                    lng = 100.0
                }
            }
            friend {
                name = "Arturo"
                age = 28
                givenName = "A"
            }
            friend {
                name = "Tiwa"
                age = 30
                streetName = "Lil T"
            }
        }
    }
}
