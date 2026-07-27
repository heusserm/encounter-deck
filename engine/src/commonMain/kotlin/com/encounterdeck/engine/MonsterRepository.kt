package com.encounterdeck.engine

import kotlin.math.abs

/**
 * Source of monster data. The engine depends ONLY on this interface, so today's
 * bundled read-only monster list can be swapped for a SQLite- or cloud-backed
 * source later without touching the generator.
 */
interface MonsterRepository {
    /**
     * Monsters eligible for the given [targetCr], [type], and [location].
     *
     * A [targetCr] below 1.0 means "the fractional-CR pool" (weak monsters for
     * level-1 parties). A null [location] means "any location". If a location
     * has too few monsters at this CR, the filter falls back to any location so
     * the caller always gets a result.
     */
    fun eligible(targetCr: Double, type: EncounterType, location: Location?): List<Monster>
}

/** In-memory repository backed by [SeedMonsters]. Used by the app and by tests. */
class InMemoryMonsterRepository(
    private val all: List<Monster> = SeedMonsters.ALL,
) : MonsterRepository {

    override fun eligible(targetCr: Double, type: EncounterType, location: Location?): List<Monster> {
        val byCr = eligibleByCr(targetCr)
        if (location == null) return byCr
        // Fall back to any location at this CR when the location slot is sparse.
        return byCr.filter { location in it.locations }.ifEmpty { byCr }
    }

    private fun eligibleByCr(targetCr: Double): List<Monster> {
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
