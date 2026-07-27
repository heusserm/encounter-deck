package com.encounterdeck.engine

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * The heart of EncounterDeck. Turns a request into a scaled encounter card.
 *
 * The algorithm (5e / 5.5e compatible):
 *
 * ```
 * difficultyRoll = uniform(tier.minRoll .. tier.maxRoll)
 * Power          = partyLevel * numPlayers * difficultyRoll / 4
 * targetCR       = max(0, partyLevel - 1)          // level 1 -> fractional pool
 * monster        = a random eligible monster near targetCR
 * numMonsters    = round(Power / monster.cr), min 1
 * XP             = monster.xp * numMonsters
 * treasure       = SRD individual treasure rolled once per monster
 * ```
 *
 * The `/ 4` normalizes for the fact that a CR-X monster is calibrated as a fair
 * challenge for four level-X characters, so counts stay balanced rather than deadly.
 *
 * [random] is injectable so tests are deterministic.
 */
class EncounterGenerator(
    private val monsters: MonsterRepository,
    private val random: Random = Random.Default,
) {
    fun generate(request: EncounterRequest): EncounterCard {
        require(request.partyLevel >= 1) { "partyLevel must be >= 1, was ${request.partyLevel}" }
        require(request.numPlayers >= 1) { "numPlayers must be >= 1, was ${request.numPlayers}" }

        val tier = request.difficulty
        val difficultyRoll = tier.minRoll + random.nextDouble() * (tier.maxRoll - tier.minRoll)

        if (request.type == EncounterType.BIG_BAD) {
            return generateBigBad(request, tier, difficultyRoll)
        }

        val power = request.partyLevel * request.numPlayers * difficultyRoll / 4.0

        val targetCr = max(0, request.partyLevel - 1).toDouble()
        val eligible = monsters.eligible(targetCr, request.type, request.location)
        require(eligible.isNotEmpty()) {
            "No eligible monsters for targetCR ~$targetCr, type ${request.type}, location ${request.location}"
        }
        val monster = eligible[random.nextInt(eligible.size)]

        val count = max(1, (power / monster.cr).roundToInt())

        // Each monster rolls its own hit points, so a group has a spread of HP.
        val hitPoints = List(count) { monster.hitDice.roll(random) }
        val totalXp = monster.xp * count
        var treasure = Treasure.NONE
        repeat(count) { treasure += TreasureTables.rollIndividual(monster.cr, random) }
        val specialLoot = LootTables.rollSpecialLoot(monster.cr, random)

        return EncounterCard(
            type = request.type,
            partyLevel = request.partyLevel,
            numPlayers = request.numPlayers,
            difficulty = tier,
            difficultyRoll = difficultyRoll,
            power = power,
            groups = listOf(MonsterGroup(monster, hitPoints)),
            totalXp = totalXp,
            treasure = treasure,
            specialLoot = specialLoot,
        )
    }

    /**
     * A single, tougher solo opponent. Instead of scaling a group, we pick ONE
     * monster whose CR is above the party — the harder the difficulty, the bigger
     * the CR bump (Explorer +1 … Honour +4). Bosses roll richer loot.
     */
    private fun generateBigBad(
        request: EncounterRequest,
        tier: Difficulty,
        difficultyRoll: Double,
    ): EncounterCard {
        val bump = when (tier) {
            Difficulty.EXPLORER -> 1
            Difficulty.BALANCED -> 2
            Difficulty.TACTICIAN -> 3
            Difficulty.HONOUR -> 4
        }
        val bossCr = (request.partyLevel + bump).toDouble()
        val eligible = monsters.eligible(bossCr, request.type, request.location)
        require(eligible.isNotEmpty()) {
            "No eligible boss for CR ~$bossCr, location ${request.location}"
        }
        val boss = eligible[random.nextInt(eligible.size)]
        val hitPoints = listOf(boss.hitDice.roll(random))

        return EncounterCard(
            type = EncounterType.BIG_BAD,
            partyLevel = request.partyLevel,
            numPlayers = request.numPlayers,
            difficulty = tier,
            difficultyRoll = difficultyRoll,
            power = boss.cr,
            groups = listOf(MonsterGroup(boss, hitPoints)),
            totalXp = boss.xp,
            treasure = TreasureTables.rollIndividual(boss.cr, random),
            specialLoot = LootTables.rollSpecialLoot(boss.cr, random),
        )
    }
}
