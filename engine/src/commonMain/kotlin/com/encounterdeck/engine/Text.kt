package com.encounterdeck.engine

/** A small English pluralizer for monster names on cards (Wolf -> Wolves, Blight -> Blights). */
fun pluralize(word: String, count: Int): String {
    if (count == 1) return word
    val lower = word.lowercase()
    return when {
        lower.endsWith("fe") -> word.dropLast(2) + "ves"   // Knife -> Knives
        lower.endsWith("f") -> word.dropLast(1) + "ves"    // Wolf -> Wolves
        lower.endsWith("y") && word.length >= 2 &&
            word[word.length - 2].lowercaseChar() !in "aeiou" -> word.dropLast(1) + "ies"
        lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("z") ||
            lower.endsWith("ch") || lower.endsWith("sh") -> word + "es"
        else -> word + "s"
    }
}

/** Plain-English count label, e.g. "2 Vine Blights", "1 Ogre". */
fun countLabel(name: String, count: Int): String = "$count ${pluralize(name, count)}"
