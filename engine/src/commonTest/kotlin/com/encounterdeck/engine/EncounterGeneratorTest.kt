package com.encounterdeck.engine

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EncounterGeneratorTest {

    private fun generator(random: Random) =
        EncounterGenerator(InMemoryMonsterRepository(), random)

    @Test
    fun `count and XP follow the formula`() {
        // doubleValue drives the difficulty roll; intValue picks the first eligible monster.
        val rng = ScriptedRandom(doubleValue = 0.5, intValue = 0)
        val card = generator(rng).generate(EncounterRequest(3, 4, Difficulty.BALANCED))

        val group = card.groups.single()
        val expectedPower = 3 * 4 * card.difficultyRoll / 4.0
        val expectedCount = max(1, (expectedPower / group.monster.cr).roundToInt())

        assertEquals(expectedPower, card.power, 1e-9)
        assertEquals(expectedCount, group.count)
        assertEquals(group.monster.xp * expectedCount, card.totalXp)
    }

    @Test
    fun `level 3 party fights CR 2 monsters at partyLevel minus 1`() {
        val gen = generator(Random(7))
        repeat(50) {
            val card = gen.generate(EncounterRequest(3, 4, Difficulty.TACTICIAN))
            assertEquals(2.0, card.groups.single().monster.cr)
        }
    }

    @Test
    fun `level 1 party draws from the fractional-CR pool`() {
        val gen = generator(Random(3))
        repeat(50) {
            val card = gen.generate(EncounterRequest(1, 4, Difficulty.BALANCED))
            val cr = card.groups.single().monster.cr
            assertTrue(cr in 0.001..0.999, "expected fractional CR, got $cr")
        }
    }

    @Test
    fun `level 1 balanced party produces a swarm of weak monsters`() {
        // intValue = 0 -> first fractional monster (Stirge, CR 1/8); doubleValue = 1.0 -> max roll.
        val rng = ScriptedRandom(doubleValue = 1.0, intValue = 0)
        val card = generator(rng).generate(EncounterRequest(1, 4, Difficulty.BALANCED))
        // Power = 1*4*1.1/4 = 1.1; 1.1 / 0.125 = 8.8 -> 9
        assertEquals(0.125, card.groups.single().monster.cr)
        assertEquals(9, card.totalMonsters)
    }

    @Test
    fun `location filter restricts to monsters tagged for that location`() {
        val gen = generator(Random(11))
        repeat(50) {
            val card = gen.generate(
                EncounterRequest(2, 4, Difficulty.BALANCED, location = Location.WATER),
            )
            val m = card.groups.single().monster
            assertTrue(Location.WATER in m.locations, "${m.name} is not a WATER monster")
        }
    }

    @Test
    fun `each monster in a group has its own rolled hit points`() {
        val gen = generator(Random(4))
        val group = gen.generate(EncounterRequest(1, 6, Difficulty.HONOUR)).groups.single()
        assertEquals(group.count, group.hitPoints.size)
        assertTrue(group.hitPoints.all { it >= 1 })
    }

    @Test
    fun `always at least one monster`() {
        val rng = ScriptedRandom(doubleValue = 0.0, intValue = 0)
        val card = generator(rng).generate(EncounterRequest(1, 1, Difficulty.EXPLORER))
        assertTrue(card.totalMonsters >= 1)
    }

    @Test
    fun `difficulty roll stays within the tier range`() {
        for (tier in Difficulty.entries) {
            val gen = generator(Random(42))
            repeat(200) {
                val card = gen.generate(EncounterRequest(5, 4, tier))
                assertTrue(
                    card.difficultyRoll in tier.minRoll..tier.maxRoll,
                    "${tier.name} roll ${card.difficultyRoll} out of range",
                )
            }
        }
    }

    @Test
    fun `harder tiers never produce fewer monsters on average`() {
        fun avgCount(tier: Difficulty): Double {
            val gen = generator(Random(99))
            return (1..500).map { gen.generate(EncounterRequest(4, 4, tier)).totalMonsters }
                .average()
        }
        assertTrue(avgCount(Difficulty.EXPLORER) <= avgCount(Difficulty.BALANCED))
        assertTrue(avgCount(Difficulty.BALANCED) <= avgCount(Difficulty.HONOUR))
    }

    @Test
    fun `big bad is a single monster tougher than the party`() {
        val gen = generator(Random(9))
        repeat(30) {
            val card = gen.generate(
                EncounterRequest(3, 4, Difficulty.BALANCED, EncounterType.BIG_BAD),
            )
            assertEquals(1, card.totalMonsters)
            assertTrue(card.groups.single().monster.cr >= 3.0)
        }
    }

    @Test
    fun `harder difficulty yields a tougher big bad`() {
        val gen = generator(Random(2))
        val explorer = gen.generate(
            EncounterRequest(5, 4, Difficulty.EXPLORER, EncounterType.BIG_BAD),
        ).groups.single().monster.cr
        val honour = gen.generate(
            EncounterRequest(5, 4, Difficulty.HONOUR, EncounterType.BIG_BAD),
        ).groups.single().monster.cr
        assertTrue(honour > explorer, "honour boss CR $honour should exceed explorer boss CR $explorer")
    }

    @Test
    fun `rejects invalid input`() {
        val gen = generator(Random(1))
        assertFailsWith<IllegalArgumentException> {
            gen.generate(EncounterRequest(0, 4, Difficulty.BALANCED))
        }
        assertFailsWith<IllegalArgumentException> {
            gen.generate(EncounterRequest(3, 0, Difficulty.BALANCED))
        }
    }
}

/**
 * A deterministic [Random] for tests: [nextDouble] always returns [doubleValue] and
 * integer draws are derived from [intValue], so both the difficulty roll and the
 * monster pick are fully controllable.
 */
private class ScriptedRandom(
    private val doubleValue: Double = 0.0,
    private val intValue: Int = 0,
) : Random() {
    override fun nextBits(bitCount: Int): Int = intValue
    override fun nextDouble(): Double = doubleValue
    override fun nextInt(until: Int): Int = if (until <= 0) 0 else intValue.mod(until)
}
