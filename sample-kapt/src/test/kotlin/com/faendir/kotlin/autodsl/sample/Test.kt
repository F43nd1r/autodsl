package com.faendir.kotlin.autodsl.sample

import org.junit.jupiter.api.Test

class Test {
    @Test
    fun test() {
        person {
            name = "Juan"
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
            }
            friend {
                name = "Tiwa"
                age = 30
            }
        }
    }
}
