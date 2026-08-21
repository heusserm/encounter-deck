package com.encounterdeck.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MonsterSearchTest {

    private val search = MonsterSearch()

    @Test
    fun `an exact name beats a name that merely contains the term`() {
        // "Hobgoblin" contains "goblin", but the typist meant the Goblin.
        assertEquals("Goblin", search.search("goblin").first().name)
    }

    @Test
    fun `autocomplete puts prefix matches ahead of substring matches`() {
        val names = search.suggest("gob").map { it.name }
        assertEquals("Goblin", names.first())
        assertTrue("Hobgoblin" in names, "expected Hobgoblin somewhere in $names")
    }

    @Test
    fun `autocomplete matches a later word, not just the start of the name`() {
        // Nothing is named "Wyrmling…", but several names end in it.
        val names = search.suggest("wyrmling", limit = 20).map { it.name }
        assertTrue(names.isNotEmpty(), "word-start matching found nothing")
        assertTrue(names.all { it.contains("Wyrmling") }, "unexpected results: $names")
    }

    @Test
    fun `autocomplete respects its limit and ignores a blank query`() {
        assertTrue(search.suggest("a", limit = 5).size <= 5)
        assertTrue(search.suggest("   ").isEmpty())
    }

    @Test
    fun `multiple terms narrow the results instead of widening them`() {
        val results = search.search("black dragon")
        assertTrue(results.isNotEmpty(), "no results for 'black dragon'")
        // Black Bear and Black Pudding match "black" but are not dragons.
        assertTrue(
            results.all { it.name.contains("Black") && it.type == "dragon" },
            "AND semantics leaked: ${results.map { it.name }}",
        )
    }

    @Test
    fun `a cr term filters rather than scores`() {
        val results = search.search("cr 1/4")
        assertTrue(results.isNotEmpty(), "no CR 1/4 monsters found")
        assertTrue(results.all { it.cr == 0.25 }, "CR filter let something through")
    }

    @Test
    fun `a cr term combines with a text term`() {
        val results = search.search("cr 1/4 undead")
        assertTrue(results.all { it.cr == 0.25 && it.type == "undead" })
    }

    @Test
    fun `searching a creature type returns that type`() {
        val results = search.search("undead")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.type == "undead" }, "non-undead in an undead search")
    }

    @Test
    fun `an unmatched query returns nothing rather than everything`() {
        assertTrue(search.search("xyzzyplugh").isEmpty())
    }

    @Test
    fun `an empty query browses the whole list alphabetically`() {
        val browsed = search.search("")
        assertEquals(SeedMonsters.ALL.size, browsed.size)
        assertEquals(browsed.map { it.name }.sorted(), browsed.map { it.name })
    }

    @Test
    fun `ids round-trip and unknown ids are null`() {
        val first = search.browse.first()
        assertEquals(first, search.byId(first.id))
        assertNull(search.byId("not-a-monster"))
    }

    @Test
    fun `every seeded monster is reachable by typing its exact name`() {
        SeedMonsters.ALL.forEach { m ->
            assertNotNull(
                search.search(m.name).firstOrNull { it.id == m.id },
                "${m.name} cannot be found by name",
            )
        }
    }

    @Test
    fun `a name search ignores matches that live elsewhere in the record`() {
        // "dragon" is a creature type as well as a name, so the wide search
        // returns dragons whose names never say "dragon" (Wyvern, Pseudodragon).
        val wide = search.search("dragon", SearchScope.EVERYTHING)
        val byName = search.search("dragon", SearchScope.NAME)
        assertTrue(byName.size < wide.size, "name scope did not narrow anything")
        assertTrue(byName.all { "dragon" in it.name.lowercase() }, "name scope leaked")
    }

    @Test
    fun `a name search does not match on attacks or immunities`() {
        // Plenty of monsters bite for poison; only a few are named for it.
        val byName = search.search("poison", SearchScope.NAME)
        assertTrue(byName.all { "poison" in it.name.lowercase() })
        assertTrue(search.search("poison", SearchScope.EVERYTHING).size > byName.size)
    }

    @Test
    fun `a name search leaves a cr term as literal text`() {
        // In EVERYTHING scope "cr 1/4" filters; as a name it matches nothing.
        assertTrue(search.search("cr 1/4", SearchScope.EVERYTHING).isNotEmpty())
        assertTrue(search.search("cr 1/4", SearchScope.NAME).isEmpty())
    }

    @Test
    fun `both scopes still find a monster by its exact name`() {
        SeedMonsters.ALL.take(40).forEach { m ->
            assertNotNull(search.search(m.name, SearchScope.NAME).firstOrNull { it.id == m.id })
            assertNotNull(search.search(m.name, SearchScope.EVERYTHING).firstOrNull { it.id == m.id })
        }
    }

    @Test
    fun `challenge ratings parse and format as fractions`() {
        assertEquals(0.125, parseCr("1/8"))
        assertEquals(0.25, parseCr("1 / 4"))
        assertEquals(5.0, parseCr("5"))
        assertNull(parseCr("hobgoblin"))
        assertNull(parseCr("1/0"))

        assertEquals("1/8", formatCr(0.125))
        assertEquals("1/2", formatCr(0.5))
        assertEquals("5", formatCr(5.0))
    }
}
