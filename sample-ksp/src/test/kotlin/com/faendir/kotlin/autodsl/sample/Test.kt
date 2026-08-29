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
            art hobby {
                name = "Painting"
                medium = "Oil"
            }
            hobby {
                game {
                    name = "Chess"
                    online = true
                }
            }
            friend {
                name = "Arturo"
                age = 28
                givenName = "A"
                sport hobby {
                    name = "Football"
                    teamSize = 11
                }
            }
            friend {
                name = "Tiwa"
                age = 30
                streetName = "Lil T"
                other hobby {
                    name = "Programming"
                    data = 1337
                }
                hobby {
                    other {
                        name = "Dancing"
                        data = "I like to dance"
                    }
                }
            }
        }
    }
}