package com.encounterdeck.engine

import kotlin.random.Random

/**
 * Extra, describable loot beyond coins — gemstones, art objects, potions, and
 * magic items — rolled by CR tier. This is a flavorful approximation of the SRD
 * Treasure Hoard tables (not an exact reproduction): one roll per encounter,
 * with richer results at higher CR. Gem/art/potion/item names are SRD examples.
 */
object LootTables {

    fun rollSpecialLoot(cr: Double, random: Random): List<String> {
        val loot = mutableListOf<String>()
        when {
            cr <= 4.0 -> {
                if (chance(random, 35)) loot += gems(random, count = 1 + random.nextInt(4), value = 10, GEMS_10)
                if (chance(random, 12)) loot += art(random, 25, ART_25)
                if (chance(random, 18)) loot += potion(random, COMMON_POTIONS)
                if (chance(random, 6)) loot += magic(random, "common", COMMON_ITEMS)
            }
            cr <= 10.0 -> {
                if (chance(random, 45)) loot += gems(random, count = 2 + random.nextInt(4), value = 50, GEMS_50)
                if (chance(random, 20)) loot += art(random, 250, ART_250)
                if (chance(random, 30)) loot += potion(random, COMMON_POTIONS)
                if (chance(random, 25)) loot += magic(random, "uncommon", UNCOMMON_ITEMS)
            }
            cr <= 16.0 -> {
                if (chance(random, 55)) loot += gems(random, count = 2 + random.nextInt(5), value = 100, GEMS_100)
                if (chance(random, 30)) loot += art(random, 750, ART_750)
                if (chance(random, 30)) loot += potion(random, GREATER_POTIONS)
                if (chance(random, 45)) loot += magic(random, "rare", RARE_ITEMS)
            }
            else -> {
                if (chance(random, 65)) loot += gems(random, count = 3 + random.nextInt(6), value = 1000, GEMS_1000)
                if (chance(random, 40)) loot += art(random, 2500, ART_2500)
                if (chance(random, 40)) loot += potion(random, GREATER_POTIONS)
                if (chance(random, 65)) loot += magic(random, "very rare", VERY_RARE_ITEMS)
                if (chance(random, 30)) loot += magic(random, "rare", RARE_ITEMS)
            }
        }
        return loot
    }

    private fun chance(random: Random, percent: Int) = random.nextInt(100) < percent

    private fun pick(random: Random, list: List<String>) = list[random.nextInt(list.size)]

    private fun gems(random: Random, count: Int, value: Int, pool: List<String>): String {
        val kind = pick(random, pool)
        return if (count == 1) "a $value gp gemstone ($kind)"
        else "$count $value gp gemstones ($kind)"
    }

    private fun art(random: Random, value: Int, pool: List<String>) =
        "an art object: ${pick(random, pool)} (~$value gp)"

    private fun potion(random: Random, pool: List<String>) = pick(random, pool)

    private fun magic(random: Random, rarity: String, pool: List<String>) =
        "magic item ($rarity): ${pick(random, pool)}"

    // --- SRD gemstone examples by value ---
    private val GEMS_10 = listOf("azurite", "banded agate", "blue quartz", "hematite", "malachite", "obsidian", "tiger eye", "turquoise")
    private val GEMS_50 = listOf("bloodstone", "carnelian", "chalcedony", "citrine", "jasper", "moonstone", "onyx", "sardonyx", "zircon")
    private val GEMS_100 = listOf("amber", "amethyst", "chrysoberyl", "coral", "garnet", "jade", "jet", "pearl", "spinel", "tourmaline")
    private val GEMS_1000 = listOf("black opal", "blue sapphire", "emerald", "fire opal", "opal", "star ruby", "star sapphire")

    // --- SRD art object examples by value ---
    private val ART_25 = listOf("a silver ewer", "a carved bone statuette", "a small gold bracelet", "a black velvet mask stitched with silver thread", "a copper chalice with silver filigree")
    private val ART_250 = listOf("a gold ring set with bloodstones", "a carved ivory statuette", "a large gold bracelet", "a silver necklace with a gemstone pendant", "a bronze crown")
    private val ART_750 = listOf("a silver chalice set with moonstones", "a gold circlet set with four aquamarines", "a ceremonial electrum dagger with a black pearl in the pommel", "a jeweled anklet")
    private val ART_2500 = listOf("a gold jewelry box with platinum filigree", "a painted gold child's sarcophagus", "a jeweled gold crown", "a platinum bracelet set with a sapphire")

    // --- Potions ---
    private val COMMON_POTIONS = listOf("Potion of Healing", "Potion of Climbing", "Potion of Animal Friendship", "Oil of Slipperiness", "Potion of Water Breathing")
    private val GREATER_POTIONS = listOf("Potion of Greater Healing", "Potion of Fire Breath", "Potion of Heroism", "Potion of Invisibility", "Potion of Superior Healing")

    // --- Magic items by rarity (SRD examples) ---
    private val COMMON_ITEMS = listOf("a +1 ammunition (10)", "Driftglobe", "Sending Stones", "a spell scroll (cantrip)")
    private val UNCOMMON_ITEMS = listOf("Bag of Holding", "Cloak of Elvenkind", "Boots of Elvenkind", "Wand of Magic Missiles", "Immovable Rod", "Goggles of Night", "Gauntlets of Ogre Power", "a +1 weapon")
    private val RARE_ITEMS = listOf("Flame Tongue", "Cloak of Protection", "Ring of Protection", "Wand of Fireballs", "Boots of Speed", "a +2 weapon", "Cloak of the Bat")
    private val VERY_RARE_ITEMS = listOf("a +3 weapon", "Rod of Absorption", "Staff of Fire", "Ring of Free Action", "Manual of Bodily Health")
}
