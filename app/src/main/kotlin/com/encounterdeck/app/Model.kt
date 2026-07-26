package com.encounterdeck.app

import kotlinx.serialization.Serializable

/** Client-side mirror of the backend's /generate response. */
@Serializable
data class CardResponse(
    val partyLevel: Int,
    val numPlayers: Int,
    val difficulty: String,
    val type: String,
    val location: String,
    val difficultyRoll: Double,
    val power: Double,
    val totalMonsters: Int,
    val totalXp: Int,
    val monsters: List<MonsterGroupResp>,
    val treasure: TreasureResp,
)

@Serializable
data class MonsterGroupResp(
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
data class TreasureResp(
    val pp: Int,
    val gp: Int,
    val ep: Int,
    val sp: Int,
    val cp: Int,
    val totalCopperValue: Long,
)
