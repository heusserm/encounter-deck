package com.encounterdeck.engine

import com.encounterdeck.engine.Location.CASTLE
import com.encounterdeck.engine.Location.DUNGEON
import com.encounterdeck.engine.Location.MOUNTAINS
import com.encounterdeck.engine.Location.TRAIL
import com.encounterdeck.engine.Location.WATER
import com.encounterdeck.engine.Location.WOODS
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

/** In-memory repository backed by [SeedMonsters]. Used by the POC and by tests. */
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

/**
 * A hand-entered SRD monster seed spanning CR 1/8 through CR 9, with ~19 options
 * for level-1 parties (fractional CR) and ~15 for level 2 (CR 1), tagged by
 * location. Stats are SRD values entered by hand — spot-check before release.
 */
object SeedMonsters {
    val ALL: List<Monster> = listOf(
        // ============ Fractional CR — level-1 pool (Stirge must stay first) ============
        Monster(
            id = "stirge", name = "Stirge", cr = 0.125, xp = 25,
            hitDice = HitDice(1, 4, 0), ac = 14, size = "Tiny", type = "beast",
            attacks = listOf("Blood Drain +5 (1d4+3 piercing, then attaches and drains 1d4 blood/turn)"),
            locations = setOf(WOODS, WATER, DUNGEON),
        ),
        Monster(
            id = "blood-hawk", name = "Blood Hawk", cr = 0.125, xp = 25,
            hitDice = HitDice(2, 6, 0), ac = 12, size = "Small", type = "beast",
            attacks = listOf("Beak +4 (1d4+2 piercing; advantage vs. targets below half HP)"),
            locations = setOf(MOUNTAINS, WOODS, TRAIL),
        ),
        Monster(
            id = "giant-rat", name = "Giant Rat", cr = 0.125, xp = 25,
            hitDice = HitDice(2, 6, 0), ac = 12, size = "Small", type = "beast",
            attacks = listOf("Bite +4 (1d4+2 piercing; advantage in a pack)"),
            locations = setOf(DUNGEON, CASTLE),
        ),
        Monster(
            id = "kobold", name = "Kobold", cr = 0.125, xp = 25,
            hitDice = HitDice(2, 6, -2), ac = 12, size = "Small", type = "humanoid",
            attacks = listOf(
                "Dagger +4 (1d4+2 piercing)",
                "Sling +4 (1d4+2 bludgeoning, ranged)",
            ),
            locations = setOf(DUNGEON, MOUNTAINS),
        ),
        Monster(
            id = "bandit", name = "Bandit", cr = 0.125, xp = 25,
            hitDice = HitDice(2, 8, 0), ac = 12, size = "Medium", type = "humanoid",
            attacks = listOf(
                "Scimitar +3 (1d6+1 slashing)",
                "Light Crossbow +3 (1d8+1 piercing, ranged)",
            ),
            locations = setOf(TRAIL, WOODS, CASTLE),
        ),
        Monster(
            id = "cultist", name = "Cultist", cr = 0.125, xp = 25,
            hitDice = HitDice(2, 8, 0), ac = 12, size = "Medium", type = "humanoid",
            attacks = listOf("Scimitar +3 (1d6+1 slashing)"),
            locations = setOf(DUNGEON, CASTLE),
        ),
        Monster(
            id = "poisonous-snake", name = "Poisonous Snake", cr = 0.125, xp = 25,
            hitDice = HitDice(1, 4, 0), ac = 13, size = "Tiny", type = "beast",
            attacks = listOf("Bite +5 (1 piercing + 2d4 poison, DC 11 Con half)"),
            locations = setOf(WATER, WOODS, TRAIL),
        ),
        Monster(
            id = "guard", name = "Guard", cr = 0.125, xp = 25,
            hitDice = HitDice(2, 8, 2), ac = 16, size = "Medium", type = "humanoid",
            attacks = listOf("Spear +3 (1d6+1 piercing)"),
            locations = setOf(CASTLE, TRAIL),
        ),
        Monster(
            id = "goblin", name = "Goblin", cr = 0.25, xp = 50,
            hitDice = HitDice(2, 6, 0), ac = 15, size = "Small", type = "humanoid",
            attacks = listOf(
                "Scimitar +4 (1d6+2 slashing)",
                "Shortbow +4 (1d6+2 piercing, ranged)",
            ),
            locations = setOf(WOODS, DUNGEON, MOUNTAINS),
        ),
        Monster(
            id = "skeleton", name = "Skeleton", cr = 0.25, xp = 50,
            hitDice = HitDice(2, 8, 4), ac = 13, size = "Medium", type = "undead",
            damageImmunities = listOf("poison"),
            conditionImmunities = listOf("exhaustion", "poisoned"),
            attacks = listOf(
                "Shortsword +4 (1d6+2 piercing)",
                "Shortbow +4 (1d6+2 piercing, ranged)",
            ),
            locations = setOf(DUNGEON, CASTLE),
        ),
        Monster(
            id = "zombie", name = "Zombie", cr = 0.25, xp = 50,
            hitDice = HitDice(3, 8, 9), ac = 8, size = "Medium", type = "undead",
            damageImmunities = listOf("poison"),
            conditionImmunities = listOf("poisoned"),
            attacks = listOf(
                "Slam +3 (1d6+1 bludgeoning)",
                "Undead Fortitude: DC 5+damage Con save to drop to 1 HP instead of 0",
            ),
            locations = setOf(DUNGEON, CASTLE),
        ),
        Monster(
            id = "wolf", name = "Wolf", cr = 0.25, xp = 50,
            hitDice = HitDice(2, 8, 2), ac = 13, size = "Medium", type = "beast",
            attacks = listOf("Bite +4 (2d4+2 piercing; DC 11 Str or knocked prone)"),
            locations = setOf(WOODS, MOUNTAINS, TRAIL),
        ),
        Monster(
            id = "giant-frog", name = "Giant Frog", cr = 0.25, xp = 50,
            hitDice = HitDice(4, 8, 0), ac = 11, size = "Medium", type = "beast",
            attacks = listOf("Bite +3 (1d6+1 piercing, grapples DC 11; can swallow Small creatures)"),
            locations = setOf(WATER),
        ),
        Monster(
            id = "vine-blight", name = "Vine Blight", cr = 0.5, xp = 100,
            hitDice = HitDice(4, 8, 8), ac = 12, size = "Medium", type = "plant",
            conditionImmunities = listOf("blinded", "deafened"),
            attacks = listOf(
                "Constrict +4 (2d6+2 bludgeoning, grapples DC 12)",
                "Entangling Plants (recharge 5–6): restrains creatures within 15 ft.",
            ),
            locations = setOf(WOODS, WATER),
        ),
        Monster(
            id = "hobgoblin", name = "Hobgoblin", cr = 0.5, xp = 100,
            hitDice = HitDice(2, 8, 2), ac = 18, size = "Medium", type = "humanoid",
            attacks = listOf(
                "Longsword +3 (1d8+1 slashing; +2d6 with an ally adjacent)",
                "Longbow +3 (1d8+1 piercing, ranged)",
            ),
            locations = setOf(DUNGEON, CASTLE, MOUNTAINS),
        ),
        Monster(
            id = "black-bear", name = "Black Bear", cr = 0.5, xp = 100,
            hitDice = HitDice(3, 8, 6), ac = 11, size = "Medium", type = "beast",
            attacks = listOf(
                "Multiattack: bite + claws",
                "Bite +3 (1d6+1 piercing)",
                "Claws +3 (2d4+1 slashing)",
            ),
            locations = setOf(WOODS, MOUNTAINS),
        ),
        Monster(
            id = "gnoll", name = "Gnoll", cr = 0.5, xp = 100,
            hitDice = HitDice(5, 8, 0), ac = 15, size = "Medium", type = "humanoid",
            attacks = listOf(
                "Spear +4 (1d6+2 piercing)",
                "Bite +4 (1d4+2 piercing)",
                "Longbow +3 (1d8+1 piercing, ranged)",
            ),
            locations = setOf(TRAIL, MOUNTAINS),
        ),
        Monster(
            id = "crocodile", name = "Crocodile", cr = 0.5, xp = 100,
            hitDice = HitDice(3, 10, 3), ac = 12, size = "Large", type = "beast",
            attacks = listOf("Bite +4 (1d10+2 piercing, grapples DC 12)"),
            locations = setOf(WATER),
        ),
        Monster(
            id = "sahuagin", name = "Sahuagin", cr = 0.5, xp = 100,
            hitDice = HitDice(4, 8, 4), ac = 12, size = "Medium", type = "humanoid",
            attacks = listOf(
                "Multiattack: bite + 2 claws (or spear)",
                "Bite +2 (1d4+1 piercing)",
                "Claws +5 (1d6+3 slashing)",
            ),
            locations = setOf(WATER),
        ),

        // ============ CR 1 — level-2 pool ============
        Monster(
            id = "bugbear", name = "Bugbear", cr = 1.0, xp = 200,
            hitDice = HitDice(5, 8, 5), ac = 16, size = "Medium", type = "humanoid",
            attacks = listOf(
                "Morningstar +4 (2d8+2 piercing; +2d6 on a surprise hit)",
                "Javelin +4 (2d6+2 piercing, ranged)",
            ),
            locations = setOf(WOODS, DUNGEON, MOUNTAINS),
        ),
        Monster(
            id = "dire-wolf", name = "Dire Wolf", cr = 1.0, xp = 200,
            hitDice = HitDice(5, 10, 10), ac = 14, size = "Large", type = "beast",
            attacks = listOf("Bite +5 (2d6+3 piercing; DC 13 Str or knocked prone)"),
            locations = setOf(WOODS, MOUNTAINS, TRAIL),
        ),
        Monster(
            id = "giant-spider", name = "Giant Spider", cr = 1.0, xp = 200,
            hitDice = HitDice(4, 10, 4), ac = 14, size = "Large", type = "beast",
            attacks = listOf(
                "Bite +5 (1d8+3 piercing + 2d8 poison, DC 11 Con)",
                "Web (recharge 5–6): restrains, DC 12 Str to break free",
            ),
            locations = setOf(DUNGEON, WOODS),
        ),
        Monster(
            id = "brown-bear", name = "Brown Bear", cr = 1.0, xp = 200,
            hitDice = HitDice(7, 10, 14), ac = 11, size = "Large", type = "beast",
            attacks = listOf(
                "Multiattack: bite + claws",
                "Bite +6 (1d8+4 piercing)",
                "Claws +6 (2d6+4 slashing)",
            ),
            locations = setOf(WOODS, MOUNTAINS),
        ),
        Monster(
            id = "ghoul", name = "Ghoul", cr = 1.0, xp = 200,
            hitDice = HitDice(5, 8, 0), ac = 12, size = "Medium", type = "undead",
            damageImmunities = listOf("poison"),
            conditionImmunities = listOf("charmed", "exhaustion", "poisoned"),
            attacks = listOf(
                "Multiattack: bite + claws",
                "Bite +2 (2d6+2 piercing)",
                "Claws +4 (2d4+2 slashing; DC 10 Con or paralyzed 1 min)",
            ),
            locations = setOf(DUNGEON, CASTLE),
        ),
        Monster(
            id = "giant-eagle", name = "Giant Eagle", cr = 1.0, xp = 200,
            hitDice = HitDice(4, 10, 4), ac = 13, size = "Large", type = "beast",
            attacks = listOf(
                "Multiattack: beak + talons",
                "Beak +5 (1d6+3 piercing)",
                "Talons +5 (2d6+3 slashing)",
            ),
            locations = setOf(MOUNTAINS, TRAIL),
        ),
        Monster(
            id = "harpy", name = "Harpy", cr = 1.0, xp = 200,
            hitDice = HitDice(7, 8, 7), ac = 11, size = "Medium", type = "monstrosity",
            attacks = listOf(
                "Multiattack: 2 claws + club",
                "Claws +3 (2d4+1 slashing)",
                "Luring Song: DC 11 Wis save or charmed and drawn toward the harpy",
            ),
            locations = setOf(MOUNTAINS, WATER),
        ),
        Monster(
            id = "lion", name = "Lion", cr = 1.0, xp = 200,
            hitDice = HitDice(4, 10, 4), ac = 12, size = "Large", type = "beast",
            attacks = listOf(
                "Multiattack: bite + claw",
                "Bite +5 (1d8+3 piercing)",
                "Claw +5 (1d6+3 slashing)",
                "Pounce: DC 13 Str or knocked prone",
            ),
            locations = setOf(TRAIL, WOODS),
        ),
        Monster(
            id = "tiger", name = "Tiger", cr = 1.0, xp = 200,
            hitDice = HitDice(5, 10, 10), ac = 12, size = "Large", type = "beast",
            attacks = listOf(
                "Multiattack: bite + claw",
                "Bite +5 (1d10+3 piercing)",
                "Claw +5 (1d8+3 slashing)",
            ),
            locations = setOf(WOODS),
        ),
        Monster(
            id = "specter", name = "Specter", cr = 1.0, xp = 200,
            hitDice = HitDice(5, 8, 0), ac = 12, size = "Medium", type = "undead",
            damageImmunities = listOf("necrotic", "poison"),
            conditionImmunities = listOf(
                "charmed", "exhaustion", "frightened", "grappled", "paralyzed",
                "petrified", "poisoned", "prone", "restrained", "unconscious",
            ),
            attacks = listOf("Life Drain +4 (3d6 necrotic; target's max HP is reduced)"),
            locations = setOf(DUNGEON, CASTLE),
        ),
        Monster(
            id = "imp", name = "Imp", cr = 1.0, xp = 200,
            hitDice = HitDice(3, 4, 3), ac = 13, size = "Tiny", type = "fiend",
            damageImmunities = listOf("fire", "poison"),
            conditionImmunities = listOf("poisoned"),
            attacks = listOf(
                "Sting +5 (1d4+3 piercing + 3d6 poison, DC 11 Con half)",
                "Invisibility & shapechange",
            ),
            locations = setOf(DUNGEON, CASTLE),
        ),
        Monster(
            id = "giant-octopus", name = "Giant Octopus", cr = 1.0, xp = 200,
            hitDice = HitDice(8, 10, 8), ac = 11, size = "Large", type = "beast",
            attacks = listOf(
                "Tentacles +5 (2d6+3 bludgeoning, grapples DC 16)",
                "Ink Cloud (recharge 6): heavily obscures a 20-ft cube underwater",
            ),
            locations = setOf(WATER),
        ),
        Monster(
            id = "animated-armor", name = "Animated Armor", cr = 1.0, xp = 200,
            hitDice = HitDice(6, 8, 6), ac = 18, size = "Medium", type = "construct",
            damageImmunities = listOf("poison", "psychic"),
            conditionImmunities = listOf(
                "blinded", "charmed", "deafened", "exhaustion", "frightened",
                "paralyzed", "petrified", "poisoned",
            ),
            attacks = listOf(
                "Multiattack: two slams",
                "Slam +4 (1d6+2 bludgeoning)",
            ),
            locations = setOf(CASTLE, DUNGEON),
        ),
        Monster(
            id = "hippogriff", name = "Hippogriff", cr = 1.0, xp = 200,
            hitDice = HitDice(3, 10, 3), ac = 11, size = "Large", type = "monstrosity",
            attacks = listOf(
                "Multiattack: beak + claws",
                "Beak +5 (1d10+3 piercing)",
                "Claws +5 (2d6+3 slashing)",
            ),
            locations = setOf(MOUNTAINS, TRAIL),
        ),
        Monster(
            id = "death-dog", name = "Death Dog", cr = 1.0, xp = 200,
            hitDice = HitDice(6, 8, 12), ac = 12, size = "Medium", type = "monstrosity",
            attacks = listOf(
                "Multiattack: two bites (two heads)",
                "Bite +4 (1d6+2 piercing; DC 12 Con or a wasting disease)",
            ),
            locations = setOf(TRAIL, DUNGEON),
        ),

        // ============ CR 2 ============
        Monster(
            id = "ogre", name = "Ogre", cr = 2.0, xp = 450,
            hitDice = HitDice(7, 10, 21), ac = 11, size = "Large", type = "giant",
            attacks = listOf(
                "Greatclub +6 (2d8+4 bludgeoning)",
                "Javelin +6 (2d6+4 piercing, ranged)",
            ),
            locations = setOf(MOUNTAINS, DUNGEON, WOODS),
        ),
        Monster(
            id = "griffon", name = "Griffon", cr = 2.0, xp = 450,
            hitDice = HitDice(7, 10, 21), ac = 12, size = "Large", type = "monstrosity",
            attacks = listOf(
                "Multiattack: beak + claws",
                "Beak +6 (1d8+4 piercing)",
                "Claws +6 (2d6+4 slashing)",
            ),
            locations = setOf(MOUNTAINS, TRAIL),
        ),
        Monster(
            id = "gnoll-pack-lord", name = "Gnoll Pack Lord", cr = 2.0, xp = 450,
            hitDice = HitDice(9, 8, 9), ac = 15, size = "Medium", type = "humanoid",
            attacks = listOf(
                "Multiattack: two weapon attacks",
                "Spear +4 (1d6+2 piercing)",
                "Bite +4 (1d4+2 piercing)",
            ),
            locations = setOf(TRAIL, MOUNTAINS),
        ),

        // ============ CR 3 ============
        Monster(
            id = "owlbear", name = "Owlbear", cr = 3.0, xp = 700,
            hitDice = HitDice(7, 10, 21), ac = 13, size = "Large", type = "monstrosity",
            attacks = listOf(
                "Multiattack: beak + claws",
                "Beak +7 (1d10+5 piercing)",
                "Claws +7 (2d8+5 slashing)",
            ),
            locations = setOf(WOODS, DUNGEON),
        ),
        Monster(
            id = "manticore", name = "Manticore", cr = 3.0, xp = 700,
            hitDice = HitDice(8, 10, 24), ac = 14, size = "Large", type = "monstrosity",
            attacks = listOf(
                "Multiattack: 3 attacks (bite + 2 claws, or 3 tail spikes)",
                "Bite +5 (1d8+3 piercing)",
                "Tail Spike +5 (1d8+3 piercing, ranged 100 ft.)",
            ),
            locations = setOf(MOUNTAINS, TRAIL),
        ),
        Monster(
            id = "werewolf", name = "Werewolf", cr = 3.0, xp = 700,
            hitDice = HitDice(9, 8, 18), ac = 12, size = "Medium", type = "humanoid",
            damageImmunities = listOf("bludgeoning, piercing, slashing from nonmagical, non-silvered weapons"),
            attacks = listOf(
                "Multiattack: two attacks (bite + claws in hybrid form)",
                "Bite +4 (1d8+2 piercing; DC 12 Con or contract lycanthropy)",
                "Claws +4 (2d4+2 slashing)",
            ),
            locations = setOf(WOODS, CASTLE, TRAIL),
        ),

        // ============ CR 4 ============
        Monster(
            id = "ettin", name = "Ettin", cr = 4.0, xp = 1100,
            hitDice = HitDice(10, 10, 30), ac = 12, size = "Large", type = "giant",
            attacks = listOf(
                "Multiattack: battleaxe + morningstar",
                "Battleaxe +7 (2d8+5 slashing)",
                "Morningstar +7 (2d8+5 piercing)",
            ),
            locations = setOf(MOUNTAINS, DUNGEON),
        ),
        Monster(
            id = "red-dragon-wyrmling", name = "Red Dragon Wyrmling", cr = 4.0, xp = 1100,
            hitDice = HitDice(10, 8, 30), ac = 17, size = "Medium", type = "dragon",
            damageImmunities = listOf("fire"),
            attacks = listOf(
                "Bite +6 (1d10+4 piercing + 1d6 fire)",
                "Fire Breath (recharge 5–6): 7d6 fire in a 15-ft cone, DC 13 Dex half",
            ),
            locations = setOf(MOUNTAINS, DUNGEON),
        ),
        Monster(
            id = "ghost", name = "Ghost", cr = 4.0, xp = 1100,
            hitDice = HitDice(10, 8, 0), ac = 11, size = "Medium", type = "undead",
            damageImmunities = listOf("cold", "necrotic", "poison"),
            conditionImmunities = listOf(
                "charmed", "exhaustion", "frightened", "grappled", "paralyzed",
                "petrified", "poisoned", "prone", "restrained",
            ),
            attacks = listOf(
                "Withering Touch +5 (4d6+3 necrotic)",
                "Horrifying Visage: DC 13 Wis save or frightened / rapidly aged",
                "Possession (recharge 6): DC 13 Cha save",
            ),
            locations = setOf(CASTLE, DUNGEON),
        ),

        // ============ CR 5 ============
        Monster(
            id = "hill-giant", name = "Hill Giant", cr = 5.0, xp = 1800,
            hitDice = HitDice(10, 12, 40), ac = 13, size = "Huge", type = "giant",
            attacks = listOf(
                "Multiattack: two greatclub attacks",
                "Greatclub +8 (3d8+5 bludgeoning)",
                "Rock +8 (3d10+5 bludgeoning, ranged)",
            ),
            locations = setOf(MOUNTAINS, TRAIL),
        ),
        Monster(
            id = "troll", name = "Troll", cr = 5.0, xp = 1800,
            hitDice = HitDice(8, 10, 40), ac = 15, size = "Large", type = "giant",
            attacks = listOf(
                "Multiattack: bite + 2 claws",
                "Bite +7 (1d6+4 piercing)",
                "Claw +7 (2d6+4 slashing)",
                "Regeneration 10 HP/turn (stopped by fire or acid)",
            ),
            locations = setOf(WATER, WOODS, DUNGEON),
        ),
        Monster(
            id = "bulette", name = "Bulette", cr = 5.0, xp = 1800,
            hitDice = HitDice(9, 10, 45), ac = 17, size = "Large", type = "monstrosity",
            attacks = listOf(
                "Bite +7 (4d12+4 piercing)",
                "Deadly Leap: DC 16 Str/Dex or knocked prone and take 3d6 damage",
            ),
            locations = setOf(TRAIL, MOUNTAINS),
        ),

        // ============ CR 6 ============
        Monster(
            id = "chimera", name = "Chimera", cr = 6.0, xp = 2300,
            hitDice = HitDice(12, 10, 48), ac = 14, size = "Large", type = "monstrosity",
            attacks = listOf(
                "Multiattack: bite + horns + claws (fire breath if available)",
                "Bite +7 (2d6+4 piercing)",
                "Horns +7 (1d12+4 bludgeoning)",
                "Fire Breath (recharge 5–6): 7d8 fire, DC 15 Dex half",
            ),
            locations = setOf(MOUNTAINS),
        ),
        Monster(
            id = "wyvern", name = "Wyvern", cr = 6.0, xp = 2300,
            hitDice = HitDice(13, 10, 39), ac = 13, size = "Large", type = "dragon",
            attacks = listOf(
                "Multiattack: bite + stinger (or claws)",
                "Bite +7 (2d6+4 piercing)",
                "Stinger +7 (2d6+4 piercing + 7d6 poison, DC 15 Con half)",
            ),
            locations = setOf(MOUNTAINS, WATER),
        ),

        // ============ CR 7 ============
        Monster(
            id = "stone-giant", name = "Stone Giant", cr = 7.0, xp = 2900,
            hitDice = HitDice(11, 12, 55), ac = 17, size = "Huge", type = "giant",
            attacks = listOf(
                "Multiattack: two greatclub attacks",
                "Greatclub +9 (3d8+6 bludgeoning)",
                "Rock +9 (4d10+6 bludgeoning, ranged)",
            ),
            locations = setOf(MOUNTAINS, DUNGEON),
        ),
        Monster(
            id = "young-black-dragon", name = "Young Black Dragon", cr = 7.0, xp = 2900,
            hitDice = HitDice(15, 10, 45), ac = 18, size = "Large", type = "dragon",
            damageImmunities = listOf("acid"),
            attacks = listOf(
                "Multiattack: bite + 2 claws",
                "Bite +7 (2d10+4 piercing + 1d8 acid)",
                "Claw +7 (2d6+4 slashing)",
                "Acid Breath (recharge 5–6): 11d8 acid in a 30-ft line, DC 14 Dex half",
            ),
            locations = setOf(WATER, DUNGEON),
        ),

        // ============ CR 8 ============
        Monster(
            id = "frost-giant", name = "Frost Giant", cr = 8.0, xp = 3900,
            hitDice = HitDice(12, 12, 60), ac = 15, size = "Huge", type = "giant",
            damageImmunities = listOf("cold"),
            attacks = listOf(
                "Multiattack: two greataxe attacks",
                "Greataxe +9 (3d12+6 slashing)",
                "Rock +9 (4d10+6 bludgeoning, ranged)",
            ),
            locations = setOf(MOUNTAINS),
        ),
        Monster(
            id = "hezrou", name = "Hezrou", cr = 8.0, xp = 3900,
            hitDice = HitDice(13, 10, 65), ac = 16, size = "Large", type = "fiend",
            damageImmunities = listOf("poison"),
            conditionImmunities = listOf("poisoned"),
            attacks = listOf(
                "Multiattack: bite + 2 claws",
                "Bite +7 (2d10+4 piercing)",
                "Claw +7 (2d6+4 slashing)",
                "Stench: DC 14 Con save or poisoned near the hezrou",
            ),
            locations = setOf(DUNGEON, WATER),
        ),

        // ============ CR 9 ============
        Monster(
            id = "fire-giant", name = "Fire Giant", cr = 9.0, xp = 5000,
            hitDice = HitDice(13, 12, 78), ac = 18, size = "Huge", type = "giant",
            damageImmunities = listOf("fire"),
            attacks = listOf(
                "Multiattack: two greatsword attacks",
                "Greatsword +11 (6d6+7 slashing)",
                "Rock +11 (4d10+7 bludgeoning, ranged)",
            ),
            locations = setOf(MOUNTAINS, DUNGEON),
        ),
        Monster(
            id = "young-blue-dragon", name = "Young Blue Dragon", cr = 9.0, xp = 5000,
            hitDice = HitDice(16, 10, 64), ac = 18, size = "Large", type = "dragon",
            damageImmunities = listOf("lightning"),
            attacks = listOf(
                "Multiattack: bite + 2 claws",
                "Bite +8 (2d10+5 piercing + 1d10 lightning)",
                "Claw +8 (2d6+5 slashing)",
                "Lightning Breath (recharge 5–6): 10d10 lightning in a 60-ft line, DC 16 Dex half",
            ),
            locations = setOf(MOUNTAINS, DUNGEON),
        ),
    )
}
