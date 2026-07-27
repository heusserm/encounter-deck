package com.encounterdeck.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class LootTablesTest {

    @Test
    fun `low CR sometimes yields special loot`() {
        val any = (1..200).any { LootTables.rollSpecialLoot(1.0, Random(it.toLong())).isNotEmpty() }
        assertTrue(any, "low CR never produced special loot across 200 rolls")
    }

    @Test
    fun `high CR yields more special loot on average than low CR`() {
        fun avg(cr: Double) =
            (1..300).map { LootTables.rollSpecialLoot(cr, Random(it.toLong())).size }.average()
        assertTrue(avg(20.0) > avg(1.0))
    }

    @Test
    fun `loot entries are never blank`() {
        (1..100).forEach { seed ->
            LootTables.rollSpecialLoot(15.0, Random(seed.toLong())).forEach {
                assertTrue(it.isNotBlank())
            }
        }
    }
}
