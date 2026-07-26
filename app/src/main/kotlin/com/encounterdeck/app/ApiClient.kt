package com.encounterdeck.app

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Thin client for the EncounterDeck backend. Later swaps to a real host / on-device engine. */
class ApiClient(private val baseUrl: String = "http://localhost:8080") {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun generate(
        partyLevel: Int,
        numPlayers: Int,
        difficulty: String,
        type: String,
    ): CardResponse = client.get("$baseUrl/generate") {
        parameter("partyLevel", partyLevel)
        parameter("numPlayers", numPlayers)
        parameter("difficulty", difficulty)
        parameter("type", type)
    }.body()
}
