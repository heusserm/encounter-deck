package com.encounterdeck.engine

import kotlin.random.Random

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

/** Where the encounter takes place. Filters which monsters are eligible. */
enum class Location {
    CASTLE,
    DUNGEON,
    WOODS,
    TRAIL,
    MOUNTAINS,
    WATER,
}

/**
 * A monster's hit-point dice, e.g. 4d8+8. Each individual monster rolls its own
 * HP, so a group of the same monster has a spread of hit points.
 */
data class HitDice(val count: Int, val die: Int, val modifier: Int) {
    /** Roll actual hit points for one monster (never below 1). */
    fun roll(random: Random): Int {
        var total = modifier
        repeat(count) { total += random.nextInt(die) + 1 }
        return maxOf(1, total)
    }

    /** The book's fixed "average" hit points for this formula. */
    val average: Int get() = maxOf(1, count * (die + 1) / 2 + modifier)

    override fun toString(): String = buildString {
        append(count).append('d').append(die)
        when {
            modifier > 0 -> append('+').append(modifier)
            modifier < 0 -> append(modifier)
        }
    }
}

/**
 * A monster stat block (5e / 5.5e SRD content).
 *
 * @param cr Challenge Rating as a Double so fractional ratings work (0.125 = CR 1/8).
 */
data class Monster(
    val id: String,
    val name: String,
    val cr: Double,
    val xp: Int,
    val hitDice: HitDice,
    val ac: Int,
    val size: String,
    val type: String,
    val armor: String? = null, // worn (lootable) armor, e.g. "leather armor, shield"; null = natural
    val damageImmunities: List<String> = emptyList(),
    val conditionImmunities: List<String> = emptyList(),
    val attacks: List<String> = emptyList(),
    val locations: Set<Location> = emptySet(),
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

/**
 * One kind of monster in the encounter, with individually-rolled hit points
 * (so [hitPoints].size is how many of this monster appear).
 */
data class MonsterGroup(val monster: Monster, val hitPoints: List<Int>) {
    val count: Int get() = hitPoints.size
}

/** Inputs to [EncounterGenerator.generate]. A null [location] means "any location". */
data class EncounterRequest(
    val partyLevel: Int,
    val numPlayers: Int,
    val difficulty: Difficulty,
    val type: EncounterType = EncounterType.WANDERING,
    val location: Location? = null,
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
