package com.encounterdeck.engine

/**
 * Name lookup and relevance search over the bundled monster list.
 *
 * Deliberately not backed by a database or a search library. The corpus is a few
 * hundred fixed, read-only records, so a linear scan is faster than any index
 * would be to load — and it keeps [engine] dependency-free on all three targets.
 *
 * Search is AND across whitespace-separated terms: every term must match
 * something, and the per-term scores add up. That makes "black dragon" narrow
 * rather than widen, which is what a typist expects.
 */
/** How much of a monster's record a search looks at. */
enum class SearchScope {
    /** Names only — "spider" will not drag in everything that bites for poison damage. */
    NAME,

    /** The whole record: name, type, size, location, attacks, armor, immunities. */
    EVERYTHING,
}

class MonsterSearch(private val all: List<Monster> = SeedMonsters.ALL) {

    /** Every monster, alphabetical — what the bestiary shows before you type. */
    val browse: List<Monster> = all.sortedBy { it.name }

    private val index: Map<String, Monster> = all.associateBy { it.id }

    fun byId(id: String): Monster? = index[id]

    /**
     * Autocomplete for the type-ahead field. Prefix matches first, then matches
     * on a later word ("dragon" -> "Adult Black Dragon"), then anything
     * containing the text. Ties break toward shorter names, so "Dragon Turtle"
     * outranks "Adult Red Dragon Wyrmling" for "dragon".
     */
    fun suggest(query: String, limit: Int = 8): List<Monster> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return all
            .mapNotNull { m ->
                val name = m.name.lowercase()
                val rank = when {
                    name.startsWith(q) -> 0
                    name.wordStartsWith(q) -> 1
                    name.contains(q) -> 2
                    else -> return@mapNotNull null
                }
                m to rank
            }
            .sortedWith(compareBy({ it.second }, { it.first.name.length }, { it.first.name }))
            .take(limit)
            .map { it.first }
    }

    /**
     * Relevance search across name, type, size, location, attacks, armor and
     * immunities. A `cr N` term (or `cr 1/4`) is pulled out and applied as a
     * filter rather than scored, so "cr 5 dragon" means what it looks like.
     *
     * Under [SearchScope.NAME] only the name is considered and the `cr` term is
     * left alone, so a name search stays a name search.
     *
     * Returns every match, best first; an empty query returns [browse].
     */
    fun search(query: String, scope: SearchScope = SearchScope.EVERYTHING): List<Monster> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return browse

        val wide = scope == SearchScope.EVERYTHING
        val crFilter = if (wide) CR_TERM.find(q)?.groupValues?.get(1)?.let(::parseCr) else null
        val stripped = if (wide) CR_TERM.replace(q, " ") else q
        val terms = stripped.split(' ', '\t').filter { it.isNotBlank() }

        val pool = if (crFilter == null) all else all.filter { it.cr == crFilter }
        if (terms.isEmpty()) return pool.sortedBy { it.name }

        return pool
            .mapNotNull { m ->
                var total = 0
                for (term in terms) {
                    val s = score(m, term, wide)
                    if (s == 0) return@mapNotNull null   // AND: every term must land
                    total += s
                }
                m to total
            }
            .sortedWith(compareByDescending<Pair<Monster, Int>> { it.second }.thenBy { it.first.name })
            .map { it.first }
    }

    /** How well one search term matches one monster; 0 means "not at all". */
    private fun score(m: Monster, term: String, wide: Boolean): Int {
        var best = 0
        val name = m.name.lowercase()
        best = maxOf(best, when {
            name == term -> 1000
            name.startsWith(term) -> 600
            name.wordStartsWith(term) -> 450
            name.contains(term) -> 300
            else -> 0
        })
        if (!wide) return best

        val type = m.type.lowercase()
        if (type == term) best = maxOf(best, 220) else if (type.contains(term)) best = maxOf(best, 170)
        if (m.size.lowercase() == term) best = maxOf(best, 160)
        if (m.locations.any { it.name.lowercase() == term }) best = maxOf(best, 140)

        // A bare number matches Challenge Rating, so "5" finds the CR 5 monsters.
        parseCr(term)?.let { if (m.cr == it) best = maxOf(best, 130) }

        if (m.attacks.any { it.lowercase().contains(term) }) best = maxOf(best, 90)
        if (m.armor?.lowercase()?.contains(term) == true) best = maxOf(best, 70)
        if (m.damageImmunities.any { it.lowercase().contains(term) } ||
            m.conditionImmunities.any { it.lowercase().contains(term) }
        ) best = maxOf(best, 60)

        return best
    }

    private companion object {
        val CR_TERM = Regex("""\bcr\s*([0-9]+(?:\s*/\s*[0-9]+)?)""")
    }
}

/** True when [term] starts any word of this string, e.g. "dragon" in "Adult Black Dragon". */
private fun String.wordStartsWith(term: String): Boolean {
    var i = indexOf(term)
    while (i > 0) {
        if (!this[i - 1].isLetterOrDigit()) return true
        i = indexOf(term, i + 1)
    }
    return false
}

/** Parses a Challenge Rating written as "5", "1/8", "0.5" — null if it isn't one. */
internal fun parseCr(text: String): Double? {
    val t = text.trim().replace(" ", "")
    if (t.isEmpty()) return null
    if ('/' in t) {
        val (n, d) = t.split('/', limit = 2)
        val num = n.toDoubleOrNull() ?: return null
        val den = d.toDoubleOrNull() ?: return null
        return if (den == 0.0) null else num / den
    }
    return t.toDoubleOrNull()
}

/** Challenge Rating for display: fractional CRs read as "1/8", not "0.125". */
fun formatCr(cr: Double): String = when (cr) {
    0.125 -> "1/8"
    0.25 -> "1/4"
    0.5 -> "1/2"
    else -> cr.toInt().toString()
}
