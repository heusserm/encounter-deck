package com.encounterdeck.engine

/**
 * Difficulty tiers. Each tier scales the encounter's Power by a random multiplier
 * drawn uniformly from [minRoll, maxRoll]. Harder tiers push the multiplier up,
 * which means more (or tougher) monsters for the same party.
 */
enum class Difficulty(val minRoll: Double, val maxRoll: Double) {
    EXPLORER(0.5, 1.0),
    BALANCED(0.8, 1.1),
    TACTICIAN(0.9, 1.2),
    HONOUR(1.1, 1.3),
}

/** The kind of encounter to generate. Only WANDERING is implemented in the POC. */
enum class EncounterType {
    WANDERING,
    // ENVIRONMENT, TRAP, BIG_BAD — later phases
}

/**
 * A single monster stat block (5e / 5.5e SRD content).
 *
 * @param cr Challenge Rating as a Double so fractional ratings work (0.125 = CR 1/8).
 */
data class Monster(
    val id: String,
    val name: String,
    val cr: Double,
    val xp: Int,
    val hp: Int,
    val ac: Int,
    val size: String,
    val type: String,
    val environments: Set<String> = emptySet(),
)

/** Coin totals, expressed in pieces of each denomination. */
data class Treasure(
    val cp: Int = 0,
    val sp: Int = 0,
    val ep: Int = 0,
    val gp: Int = 0,
    val pp: Int = 0,
) {
    operator fun plus(other: Treasure) = Treasure(
        cp = cp + other.cp,
        sp = sp + other.sp,
        ep = ep + other.ep,
        gp = gp + other.gp,
        pp = pp + other.pp,
    )

    val isEmpty: Boolean get() = cp == 0 && sp == 0 && ep == 0 && gp == 0 && pp == 0

    /** Total value in copper pieces (for comparisons / display). */
    fun totalCopperValue(): Long =
        cp + sp * 10L + ep * 50L + gp * 100L + pp * 1000L

    companion object {
        val NONE = Treasure()
    }
}

/** One kind of monster and how many of it appear in the encounter. */
data class MonsterGroup(val monster: Monster, val count: Int)

/** Inputs to [EncounterGenerator.generate]. */
data class EncounterRequest(
    val partyLevel: Int,
    val numPlayers: Int,
    val difficulty: Difficulty,
    val type: EncounterType = EncounterType.WANDERING,
)

/** A fully generated encounter — everything the UI needs to render a card. */
data class EncounterCard(
    val type: EncounterType,
    val partyLevel: Int,
    val numPlayers: Int,
    val difficulty: Difficulty,
    val difficultyRoll: Double,
    val power: Double,
    val groups: List<MonsterGroup>,
    val totalXp: Int,
    val treasure: Treasure,
) {
    val totalMonsters: Int get() = groups.sumOf { it.count }
}
