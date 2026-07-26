package com.encounterdeck.backend

import com.encounterdeck.engine.Difficulty
import com.encounterdeck.engine.EncounterGenerator
import com.encounterdeck.engine.EncounterRequest
import com.encounterdeck.engine.EncounterType
import com.encounterdeck.engine.InMemoryMonsterRepository
import com.encounterdeck.engine.Location
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

private const val ATTRIBUTION =
    "This is a play aid for 5e-compatible games developed by Matthew Heusser (matt@xndev.com)."

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true })
    }
    install(CORS) {
        // POC: the Compose desktop client / throwaway web page may run on another origin.
        anyHost()
    }
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "bad request"))
        }
    }

    val generator = EncounterGenerator(InMemoryMonsterRepository())

    routing {
        get("/") {
            call.respondText(
                buildString {
                    appendLine("EncounterDeck API")
                    appendLine(ATTRIBUTION)
                    appendLine()
                    appendLine("Try: /generate?partyLevel=3&numPlayers=4&difficulty=balanced&location=dungeon")
                    appendLine("  difficulty: explorer | balanced | tactician | honour")
                    appendLine("  location:   any | ${Location.entries.joinToString(" | ") { it.name.lowercase() }}")
                    appendLine("  type:       wandering")
                }
            )
        }

        get("/generate") {
            val params = call.request.queryParameters
            val partyLevel = params["partyLevel"]?.toIntOrNull()
                ?: throw IllegalArgumentException("partyLevel is required (integer >= 1)")
            val numPlayers = params["numPlayers"]?.toIntOrNull()
                ?: throw IllegalArgumentException("numPlayers is required (integer >= 1)")
            val difficulty = parseDifficulty(params["difficulty"])
            val type = parseType(params["type"])
            val location = parseLocation(params["location"])

            val card = generator.generate(
                EncounterRequest(partyLevel, numPlayers, difficulty, type, location)
            )
            call.respond(card.toResponse(location = location?.name?.lowercase() ?: "any"))
        }
    }
}

private fun parseDifficulty(raw: String?): Difficulty {
    if (raw == null) return Difficulty.BALANCED
    return Difficulty.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        ?: throw IllegalArgumentException(
            "unknown difficulty '$raw'; valid: ${Difficulty.entries.joinToString(", ") { it.name.lowercase() }}"
        )
}

private fun parseType(raw: String?): EncounterType {
    if (raw == null) return EncounterType.WANDERING
    return EncounterType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        ?: throw IllegalArgumentException(
            "unknown type '$raw'; valid: ${EncounterType.entries.joinToString(", ") { it.name.lowercase() }}"
        )
}

private fun parseLocation(raw: String?): Location? {
    if (raw == null || raw.isBlank() || raw.equals("any", ignoreCase = true)) return null
    return Location.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        ?: throw IllegalArgumentException(
            "unknown location '$raw'; valid: any, ${Location.entries.joinToString(", ") { it.name.lowercase() }}"
        )
}
