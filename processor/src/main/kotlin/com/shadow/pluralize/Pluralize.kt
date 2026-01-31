package com.shadow.pluralize

import com.shadow.pluralize.utils.Plurality
import java.util.regex.Pattern

fun String.pluralize(plurality: Plurality = Plurality.Singular): String {
    if (plurality == Plurality.Plural) return this

    if (plurality == Plurality.Singular) {
        return this.pluralizer()
    }

    if (this.singularizer() != this &&
        this.singularizer() + "s" != this &&
        this.singularizer().pluralizer() == this &&
        this.pluralizer() != this
    ) {
        return this
    }

    return this.pluralizer()
}

fun String.singularize(plurality: Plurality = Plurality.Plural): String {
    if (plurality == Plurality.Singular) return this

    if (plurality == Plurality.Plural) return this.singularizer()

    if (this.pluralizer() != this &&
        this + "s" != this.pluralizer() &&
        this.pluralizer().singularize() == this &&
        this.singularizer() != this
    ) {
        return this
    }

    return this.singularize()
}

fun String.pluralize(count: Int): String {
    if (count > 1) {
        return this.pluralize(Plurality.Plural)
    } else {
        return this.pluralize(Plurality.Singular)
    }
}

fun String.singularize(count: Int): String {
    if (count > 1) {
        return this.singularize(Plurality.Plural)
    } else {
        return this.singularize(Plurality.Singular)
    }
}

private fun String.pluralizer(): String {
    if (unCountable().contains(this.lowercase())) return this
    val rule = pluralizeRules().last { Pattern.compile(it.component1(), Pattern.CASE_INSENSITIVE).matcher(this).find() }
    var found = Pattern.compile(rule.component1(), Pattern.CASE_INSENSITIVE).matcher(this).replaceAll(rule.component2())
    val endsWith = exceptions().firstOrNull { this.endsWith(it.component1()) }
    if (endsWith != null) found = this.replace(endsWith.component1(), endsWith.component2())
    val exception = exceptions().firstOrNull { this.equals(it.component1(), true) }
    if (exception != null) found = exception.component2()
    return found
}

private fun String.singularizer(): String {
    if (unCountable().contains(this.lowercase())) {
        return this
    }
    val exceptions = exceptions().firstOrNull { this.equals(it.component2(), true) }

    if (exceptions != null) {
        return exceptions.component1()
    }
    val endsWith = exceptions().firstOrNull { this.endsWith(it.component2()) }

    if (endsWith != null) return this.replace(endsWith.component2(), endsWith.component1())

    try {
        if (singularizeRules().count {
                Pattern.compile(it.component1(), Pattern.CASE_INSENSITIVE).matcher(this).find()
            } == 0
        ) {
            return this
        }
        val rule =
            singularizeRules().last {
                Pattern.compile(it.component1(), Pattern.CASE_INSENSITIVE).matcher(this).find()
            }
        return Pattern.compile(rule.component1(), Pattern.CASE_INSENSITIVE).matcher(this).replaceAll(rule.component2())
    } catch (ex: IllegalArgumentException) {
        Exception("Can't singularize this word, could not find a rule to match.")
    }
    return this
}

fun unCountable(): List<String> =
    listOf(
        "equipment",
        "information",
        "rice",
        "money",
        "species",
        "series",
        "fish",
        "sheep",
        "aircraft",
        "bison",
        "flounder",
        "pliers",
        "bream",
        "gallows",
        "proceedings",
        "breeches",
        "graffiti",
        "rabies",
        "britches",
        "headquarters",
        "salmon",
        "carp",
        "herpes",
        "scissors",
        "chassis",
        "high-jinks",
        "sea-bass",
        "clippers",
        "homework",
        "cod",
        "innings",
        "shears",
        "contretemps",
        "jackanapes",
        "corps",
        "mackerel",
        "swine",
        "debris",
        "measles",
        "trout",
        "diabetes",
        "mews",
        "tuna",
        "djinn",
        "mumps",
        "whiting",
        "eland",
        "news",
        "wildebeest",
        "elk",
        "pincers",
        "sugar",
    )

fun exceptions(): List<Pair<String, String>> =
    listOf(
        "person" to "people",
        "man" to "men",
        "goose" to "geese",
        "child" to "children",
        "sex" to "sexes",
        "move" to "moves",
        "stadium" to "stadiums",
        "deer" to "deer",
        "codex" to "codices",
        "murex" to "murices",
        "silex" to "silices",
        "radix" to "radices",
        "helix" to "helices",
        "alumna" to "alumnae",
        "alga" to "algae",
        "vertebra" to "vertebrae",
        "persona" to "personae",
        "stamen" to "stamina",
        "foramen" to "foramina",
        "lumen" to "lumina",
        "afreet" to "afreeti",
        "afrit" to "afriti",
        "efreet" to "efreeti",
        "cherub" to "cherubim",
        "goy" to "goyim",
        "human" to "humans",
        "lumen" to "lumina",
        "seraph" to "seraphim",
        "Alabaman" to "Alabamans",
        "Bahaman" to "Bahamans",
        "Burman" to "Burmans",
        "German" to "Germans",
        "Hiroshiman" to "Hiroshimans",
        "Liman" to "Limans",
        "Nakayaman" to "Nakayamans",
        "Oklahoman" to "Oklahomans",
        "Panaman" to "Panamans",
        "Selman" to "Selmans",
        "Sonaman" to "Sonamans",
        "Tacoman" to "Tacomans",
        "Yakiman" to "Yakimans",
        "Yokohaman" to "Yokohamans",
        "Yuman" to "Yumans",
        "criterion" to "criteria",
        "perihelion" to "perihelia",
        "aphelion" to "aphelia",
        "phenomenon" to "phenomena",
        "prolegomenon" to "prolegomena",
        "noumenon" to "noumena",
        "organon" to "organa",
        "asyndeton" to "asyndeta",
        "hyperbaton" to "hyperbata",
        "foot" to "feet",
    )

fun pluralizeRules(): List<Pair<String, String>> =
    listOf(
        "$" to "s",
        "s$" to "s",
        "(ax|test)is$" to "$1es",
        "us$" to "i",
        "(octop|vir)us$" to "$1i",
        "(octop|vir)i$" to "$1i",
        "(alias|status)$" to "$1es",
        "(bu)s$" to "$1ses",
        "(buffal|tomat)o$" to "$1oes",
        "([ti])um$" to "$1a",
        "([ti])a$" to "$1a",
        "sis$" to "ses",
        "(,:([^f])fe|([lr])f)$" to "$1$2ves",
        "(hive)$" to "$1s",
        "([^aeiouy]|qu)y$" to "$1ies",
        "(x|ch|ss|sh)$" to "$1es",
        "(matr|vert|ind)ix|ex$" to "$1ices",
        "([m|l])ouse$" to "$1ice",
        "([m|l])ice$" to "$1ice",
        "^(ox)$" to "$1en",
        "(quiz)$" to "$1zes",
        "f$" to "ves",
        "fe$" to "ves",
        "um$" to "a",
        "on$" to "a",
        "tion" to "tions",
        "sion" to "sions",
    )

fun singularizeRules(): List<Pair<String, String>> =
    listOf(
        "s$" to "",
        "(s|si|u)s$" to "$1s",
        "(n)ews$" to "$1ews",
        "([ti])a$" to "$1um",
        "((a)naly|(b)a|(d)iagno|(p)arenthe|(p)rogno|(s)ynop|(t)he)ses$" to "$1$2sis",
        "(^analy)ses$" to "$1sis",
        "(^analy)sis$" to "$1sis",
        "([^f])ves$" to "$1fe",
        "(hive)s$" to "$1",
        "(tive)s$" to "$1",
        "([lr])ves$" to "$1f",
        "([^aeiouy]|qu)ies$" to "$1y",
        "(s)eries$" to "$1eries",
        "(m)ovies$" to "$1ovie",
        "(x|ch|ss|sh)es$" to "$1",
        "([m|l])ice$" to "$1ouse",
        "(bus)es$" to "$1",
        "(o)es$" to "$1",
        "(shoe)s$" to "$1",
        "(cris|ax|test)is$" to "$1is",
        "(cris|ax|test)es$" to "$1is",
        "(octop|vir)i$" to "$1us",
        "(octop|vir)us$" to "$1us",
        "(alias|status)es$" to "$1",
        "(alias|status)$" to "$1",
        "^(ox)en" to "$1",
        "(vert|ind)ices$" to "$1ex",
        "(matr)ices$" to "$1ix",
        "(quiz)zes$" to "$1",
        "a$" to "um",
        "i$" to "us",
        "ae$" to "a",
    )
