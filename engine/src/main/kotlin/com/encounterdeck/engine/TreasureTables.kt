package com.encounterdeck.engine

import kotlin.random.Random

/**
 * SRD "Individual Treasure" tables, keyed by the monster's CR bracket
 * (0–4, 5–10, 11–16, 17+). Roll once per monster and sum across the encounter.
 */
object TreasureTables {

    /** Roll individual treasure for a single monster of the given [cr]. */
    fun rollIndividual(cr: Double, random: Random): Treasure = when {
        cr <= 4.0 -> tier0to4(random)
        cr <= 10.0 -> tier5to10(random)
        cr <= 16.0 -> tier11to16(random)
        else -> tier17plus(random)
    }

    private fun tier0to4(random: Random): Treasure = when (d100(random)) {
        in 1..30 -> Treasure(cp = roll(5, 6, random))
        in 31..60 -> Treasure(sp = roll(4, 6, random))
        in 61..70 -> Treasure(ep = roll(3, 6, random))
        in 71..95 -> Treasure(gp = roll(3, 6, random))
        else -> Treasure(pp = roll(1, 6, random))
    }

    private fun tier5to10(random: Random): Treasure = when (d100(random)) {
        in 1..30 -> Treasure(cp = roll(4, 6, random) * 100, ep = roll(1, 6, random) * 10)
        in 31..60 -> Treasure(sp = roll(6, 6, random) * 10, gp = roll(2, 6, random) * 10)
        in 61..70 -> Treasure(ep = roll(3, 6, random) * 10, gp = roll(2, 6, random) * 10)
        in 71..95 -> Treasure(gp = roll(4, 6, random) * 10)
        else -> Treasure(gp = roll(2, 6, random) * 10, pp = roll(3, 6, random))
    }

    private fun tier11to16(random: Random): Treasure = when (d100(random)) {
        in 1..20 -> Treasure(sp = roll(4, 6, random) * 100, gp = roll(1, 6, random) * 100)
        in 21..35 -> Treasure(ep = roll(1, 6, random) * 100, gp = roll(1, 6, random) * 100)
        in 36..75 -> Treasure(gp = roll(2, 6, random) * 100, pp = roll(1, 6, random) * 10)
        else -> Treasure(gp = roll(2, 6, random) * 100, pp = roll(2, 6, random) * 10)
    }

    private fun tier17plus(random: Random): Treasure = when (d100(random)) {
        in 1..15 -> Treasure(ep = roll(2, 6, random) * 1000, gp = roll(8, 6, random) * 100)
        in 16..55 -> Treasure(gp = roll(1, 6, random) * 1000, pp = roll(1, 6, random) * 100)
        else -> Treasure(gp = roll(1, 6, random) * 1000, pp = roll(2, 6, random) * 100)
    }

    /** Roll [n] dice of [sides] each (e.g. roll(3, 6, r) == 3d6). */
    private fun roll(n: Int, sides: Int, random: Random): Int {
        var sum = 0
        repeat(n) { sum += random.nextInt(sides) + 1 }
        return sum
    }

    private fun d100(random: Random): Int = random.nextInt(100) + 1
}
