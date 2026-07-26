package com.encounterdeck.engine

import kotlin.random.Random

/** Prints a few sample encounter cards so we can eyeball the engine from the terminal. */
fun main() {
    val generator = EncounterGenerator(InMemoryMonsterRepository(), Random(1))

    val requests = listOf(
        EncounterRequest(partyLevel = 1, numPlayers = 4, difficulty = Difficulty.BALANCED),
        EncounterRequest(partyLevel = 3, numPlayers = 4, difficulty = Difficulty.TACTICIAN),
        EncounterRequest(partyLevel = 3, numPlayers = 6, difficulty = Difficulty.HONOUR),
        EncounterRequest(partyLevel = 5, numPlayers = 4, difficulty = Difficulty.EXPLORER),
        EncounterRequest(partyLevel = 8, numPlayers = 5, difficulty = Difficulty.BALANCED),
    )

    for (request in requests) {
        printCard(generator.generate(request), request)
    }
}

private fun printCard(card: EncounterCard, request: EncounterRequest) {
    println("=".repeat(56))
    println(
        "Party level ${request.partyLevel}, ${request.numPlayers} players, " +
            "${request.difficulty.name.lowercase()}  (${request.type.name.lowercase()})"
    )
    println(
        "Power ${"%.2f".format(card.power)}  " +
            "(roll ${"%.2f".format(card.difficultyRoll)})"
    )
    for (group in card.groups) {
        val m = group.monster
        println(
            "  ${group.count}x ${m.name}  " +
                "CR ${formatCr(m.cr)}, AC ${m.ac}, HP ${m.hp} each"
        )
    }
    println("  XP total: ${card.totalXp}")
    println("  Treasure: ${formatTreasure(card.treasure)}")
}

private fun formatCr(cr: Double): String = when (cr) {
    0.125 -> "1/8"
    0.25 -> "1/4"
    0.5 -> "1/2"
    else -> cr.toInt().toString()
}

private fun formatTreasure(t: Treasure): String {
    if (t.isEmpty) return "none"
    val parts = buildList {
        if (t.pp > 0) add("${t.pp} pp")
        if (t.gp > 0) add("${t.gp} gp")
        if (t.ep > 0) add("${t.ep} ep")
        if (t.sp > 0) add("${t.sp} sp")
        if (t.cp > 0) add("${t.cp} cp")
    }
    return parts.joinToString(", ")
}
