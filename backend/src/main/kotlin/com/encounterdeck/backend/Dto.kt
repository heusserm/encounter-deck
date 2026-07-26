package com.encounterdeck.backend

import com.encounterdeck.engine.EncounterCard
import com.encounterdeck.engine.Treasure
import com.encounterdeck.engine.countLabel
import kotlinx.serialization.Serializable

/**
 * JSON-facing response shapes. These live in the backend (not the engine) so the
 * engine stays framework-free and we control the wire format independently.
 */
@Serializable
data class GenerateResponse(
    val partyLevel: Int,
    val numPlayers: Int,
    val difficulty: String,
    val type: String,
    val location: String,
    val difficultyRoll: Double,
    val power: Double,
    val totalMonsters: Int,
    val totalXp: Int,
    val monsters: List<MonsterGroupDto>,
    val treasure: TreasureDto,
)

@Serializable
data class MonsterGroupDto(
    val count: Int,
    val name: String,
    val label: String,
    val cr: String,
    val ac: Int,
    val hitDice: String,
    val hitPoints: List<Int>,
    val xp: Int,
    val size: String,
    val type: String,
    val damageImmunities: List<String>,
    val conditionImmunities: List<String>,
    val attacks: List<String>,
    val locations: List<String>,
)

@Serializable
data class TreasureDto(
    val pp: Int,
    val gp: Int,
    val ep: Int,
    val sp: Int,
    val cp: Int,
    val totalCopperValue: Long,
)

@Serializable
data class ErrorResponse(val error: String)

/** Map an engine [EncounterCard] to its JSON response shape. */
fun EncounterCard.toResponse(location: String): GenerateResponse = GenerateResponse(
    partyLevel = partyLevel,
    numPlayers = numPlayers,
    difficulty = difficulty.name.lowercase(),
    type = type.name.lowercase(),
    location = location,
    difficultyRoll = roundTo(difficultyRoll, 2),
    power = roundTo(power, 2),
    totalMonsters = totalMonsters,
    totalXp = totalXp,
    monsters = groups.map { group ->
        val m = group.monster
        MonsterGroupDto(
            count = group.count,
            name = m.name,
            label = countLabel(m.name, group.count),
            cr = formatCr(m.cr),
            ac = m.ac,
            hitDice = m.hitDice.toString(),
            hitPoints = group.hitPoints,
            xp = m.xp,
            size = m.size,
            type = m.type,
            damageImmunities = m.damageImmunities,
            conditionImmunities = m.conditionImmunities,
            attacks = m.attacks,
            locations = m.locations.map { it.name.lowercase() }.sorted(),
        )
    },
    treasure = treasure.toDto(),
)

private fun Treasure.toDto() = TreasureDto(
    pp = pp, gp = gp, ep = ep, sp = sp, cp = cp,
    totalCopperValue = totalCopperValue(),
)

private fun formatCr(cr: Double): String = when (cr) {
    0.125 -> "1/8"
    0.25 -> "1/4"
    0.5 -> "1/2"
    else -> cr.toInt().toString()
}

private fun roundTo(value: Double, decimals: Int): Double {
    var factor = 1.0
    repeat(decimals) { factor *= 10 }
    return kotlin.math.round(value * factor) / factor
}
