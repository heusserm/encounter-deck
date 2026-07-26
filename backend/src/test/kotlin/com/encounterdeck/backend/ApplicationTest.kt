package com.encounterdeck.backend

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    @Test
    fun `generate returns a card as JSON`() = testApplication {
        application { module() }
        val response = client.get("/generate?partyLevel=3&numPlayers=4&difficulty=balanced")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"totalXp\""), "missing totalXp in: $body")
        assertTrue(body.contains("\"monsters\""), "missing monsters in: $body")
        assertTrue(body.contains("\"treasure\""), "missing treasure in: $body")
    }

    @Test
    fun `defaults difficulty and type when omitted`() = testApplication {
        application { module() }
        val response = client.get("/generate?partyLevel=2&numPlayers=4")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(Regex("\"difficulty\"\\s*:\\s*\"balanced\"").containsMatchIn(body), body)
        assertTrue(Regex("\"type\"\\s*:\\s*\"wandering\"").containsMatchIn(body), body)
    }

    @Test
    fun `unknown difficulty returns 400`() = testApplication {
        application { module() }
        val response = client.get("/generate?partyLevel=3&numPlayers=4&difficulty=nope")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("unknown difficulty"))
    }

    @Test
    fun `missing required param returns 400`() = testApplication {
        application { module() }
        val response = client.get("/generate?numPlayers=4&difficulty=balanced")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `invalid party level returns 400`() = testApplication {
        application { module() }
        val response = client.get("/generate?partyLevel=0&numPlayers=4")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
