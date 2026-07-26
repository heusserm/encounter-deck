package com.encounterdeck.backend

import com.encounterdeck.engine.EncounterCard
import com.encounterdeck.engine.Treasure
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
    val cr: String,
    val ac: Int,
    val hp: Int,
    val xp: Int,
    val size: String,
    val type: String,
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
fun EncounterCard.toResponse(): GenerateResponse = GenerateResponse(
    partyLevel = partyLevel,
    numPlayers = numPlayers,
    difficulty = difficulty.name.lowercase(),
    type = type.name.lowercase(),
    difficultyRoll = roundTo(difficultyRoll, 2),
    power = roundTo(power, 2),
    totalMonsters = totalMonsters,
    totalXp = totalXp,
    monsters = groups.map { group ->
        MonsterGroupDto(
            count = group.count,
            name = group.monster.name,
            cr = formatCr(group.monster.cr),
            ac = group.monster.ac,
            hp = group.monster.hp,
            xp = group.monster.xp,
            size = group.monster.size,
            type = group.monster.type,
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
