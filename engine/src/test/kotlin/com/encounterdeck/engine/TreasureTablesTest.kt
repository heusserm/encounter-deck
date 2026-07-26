package com.encounterdeck.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class TreasureTablesTest {

    @Test
    fun `every CR bracket yields some treasure`() {
        val crByBracket = listOf(0.125, 2.0, 4.0, 5.0, 10.0, 11.0, 16.0, 17.0, 24.0)
        for (cr in crByBracket) {
            // Sample many rolls so we don't depend on one lucky/unlucky draw.
            val anyNonEmpty = (1..50).any {
                !TreasureTables.rollIndividual(cr, Random(it.toLong())).isEmpty
            }
            assertTrue(anyNonEmpty, "CR $cr never produced treasure")
        }
    }

    @Test
    fun `treasure accumulates across monsters`() {
        val one = TreasureTables.rollIndividual(2.0, Random(5))
        val summed = one + one + one
        assertTrue(summed.totalCopperValue() >= one.totalCopperValue())
    }

    @Test
    fun `high-CR treasure is generally worth more than low-CR treasure`() {
        fun avgValue(cr: Double): Double =
            (1..500).map { TreasureTables.rollIndividual(cr, Random(it.toLong())).totalCopperValue() }
                .average()

        assertTrue(
            avgValue(9.0) > avgValue(0.125),
            "expected CR 9 treasure to out-value CR 1/8 treasure",
        )
    }
}
