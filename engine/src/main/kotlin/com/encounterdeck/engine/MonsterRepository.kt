package com.encounterdeck.engine

import kotlin.math.abs

/**
 * Source of monster data. The engine depends ONLY on this interface, so today's
 * bundled read-only monster list can be swapped for a SQLite- or cloud-backed
 * source later without touching the generator.
 */
interface MonsterRepository {
    /**
     * Monsters eligible for the given [targetCr] and encounter [type].
     *
     * A [targetCr] below 1.0 means "the fractional-CR pool" (weak monsters for
     * level-1 parties — stirges, blood hawks, and so on).
     */
    fun eligible(targetCr: Double, type: EncounterType): List<Monster>
}

/** In-memory repository backed by [SeedMonsters]. Used by the POC and by tests. */
class InMemoryMonsterRepository(
    private val all: List<Monster> = SeedMonsters.ALL,
) : MonsterRepository {

    override fun eligible(targetCr: Double, type: EncounterType): List<Monster> {
        // Level-1 parties draw from the fractional pool (0 < CR < 1).
        if (targetCr < 1.0) {
            return all.filter { it.cr in FRACTIONAL_RANGE }
        }
        // Prefer an exact CR match; fall back to the nearest available CR.
        val exact = all.filter { it.cr == targetCr }
        if (exact.isNotEmpty()) return exact

        val nearestCr = all.minByOrNull { abs(it.cr - targetCr) }?.cr ?: return emptyList()
        return all.filter { it.cr == nearestCr }
    }

    private companion object {
        // 0 < CR < 1: e.g. CR 1/8 (0.125) through CR 1/2 (0.5).
        val FRACTIONAL_RANGE = 0.001..0.999
    }
}

/**
 * A small SRD monster seed spanning CR 1/8 through CR 9, enough to exercise the
 * generator for party levels 1–10 in the POC. Stats are SRD values.
 */
object SeedMonsters {
    val ALL: List<Monster> = listOf(
        // --- Fractional CR (level-1 pool) ---
        Monster("stirge", "Stirge", 0.125, 25, 2, 14, "Tiny", "beast", setOf("forest", "swamp", "cave")),
        Monster("blood-hawk", "Blood Hawk", 0.125, 25, 7, 12, "Small", "beast", setOf("hill", "mountain", "forest")),
        Monster("giant-rat", "Giant Rat", 0.125, 25, 7, 12, "Small", "beast", setOf("urban", "cave", "dungeon")),
        Monster("kobold", "Kobold", 0.125, 25, 5, 12, "Small", "humanoid", setOf("cave", "dungeon", "mountain")),
        Monster("vine-blight", "Vine Blight", 0.5, 100, 26, 12, "Medium", "plant", setOf("forest", "swamp")),

        // --- CR 1 ---
        Monster("bugbear", "Bugbear", 1.0, 200, 27, 16, "Medium", "humanoid", setOf("forest", "cave", "mountain")),
        Monster("dire-wolf", "Dire Wolf", 1.0, 200, 37, 14, "Large", "beast", setOf("forest", "hill", "grassland")),
        Monster("giant-spider", "Giant Spider", 1.0, 200, 26, 14, "Large", "beast", setOf("cave", "forest", "dungeon")),

        // --- CR 2 ---
        Monster("ogre", "Ogre", 2.0, 450, 59, 11, "Large", "giant", setOf("hill", "cave", "forest")),
        Monster("griffon", "Griffon", 2.0, 450, 59, 12, "Large", "monstrosity", setOf("mountain", "hill", "grassland")),
        Monster("gnoll-pack-lord", "Gnoll Pack Lord", 2.0, 450, 49, 15, "Medium", "humanoid", setOf("grassland", "desert")),

        // --- CR 3 ---
        Monster("owlbear", "Owlbear", 3.0, 700, 59, 13, "Large", "monstrosity", setOf("forest", "cave")),
        Monster("manticore", "Manticore", 3.0, 700, 68, 14, "Large", "monstrosity", setOf("mountain", "hill", "desert")),
        Monster("werewolf", "Werewolf", 3.0, 700, 58, 12, "Medium", "humanoid", setOf("forest", "urban", "hill")),

        // --- CR 4 ---
        Monster("ettin", "Ettin", 4.0, 1100, 85, 12, "Large", "giant", setOf("hill", "mountain", "cave")),
        Monster("red-dragon-wyrmling", "Red Dragon Wyrmling", 4.0, 1100, 75, 17, "Medium", "dragon", setOf("mountain", "cave")),
        Monster("ghost", "Ghost", 4.0, 1100, 45, 11, "Medium", "undead", setOf("urban", "dungeon")),

        // --- CR 5 ---
        Monster("hill-giant", "Hill Giant", 5.0, 1800, 105, 13, "Huge", "giant", setOf("hill", "grassland")),
        Monster("troll", "Troll", 5.0, 1800, 84, 15, "Large", "giant", setOf("swamp", "forest", "cave")),
        Monster("bulette", "Bulette", 5.0, 1800, 94, 17, "Large", "monstrosity", setOf("hill", "grassland", "desert")),

        // --- CR 6 ---
        Monster("chimera", "Chimera", 6.0, 2300, 114, 14, "Large", "monstrosity", setOf("mountain", "hill")),
        Monster("wyvern", "Wyvern", 6.0, 2300, 110, 13, "Large", "dragon", setOf("mountain", "hill", "swamp")),

        // --- CR 7 ---
        Monster("stone-giant", "Stone Giant", 7.0, 2900, 126, 17, "Huge", "giant", setOf("mountain", "cave", "hill")),
        Monster("young-black-dragon", "Young Black Dragon", 7.0, 2900, 127, 18, "Large", "dragon", setOf("swamp", "cave")),

        // --- CR 8 ---
        Monster("frost-giant", "Frost Giant", 8.0, 3900, 138, 15, "Huge", "giant", setOf("arctic", "mountain")),
        Monster("hezrou", "Hezrou", 8.0, 3900, 136, 16, "Large", "fiend", setOf("dungeon", "swamp")),

        // --- CR 9 ---
        Monster("fire-giant", "Fire Giant", 9.0, 5000, 162, 18, "Huge", "giant", setOf("mountain", "cave")),
        Monster("young-blue-dragon", "Young Blue Dragon", 9.0, 5000, 152, 18, "Large", "dragon", setOf("desert", "cave")),
    )
}
