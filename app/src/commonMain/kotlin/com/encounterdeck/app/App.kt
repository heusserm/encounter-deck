package com.encounterdeck.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.encounterdeck.engine.Difficulty
import com.encounterdeck.engine.EncounterCard
import com.encounterdeck.engine.EncounterGenerator
import com.encounterdeck.engine.EncounterRequest
import com.encounterdeck.engine.EncounterType
import com.encounterdeck.engine.InMemoryMonsterRepository
import com.encounterdeck.engine.Location
import com.encounterdeck.engine.Monster
import com.encounterdeck.engine.MonsterGroup
import com.encounterdeck.engine.MonsterSearch
import com.encounterdeck.engine.SearchScope
import com.encounterdeck.engine.Treasure
import com.encounterdeck.engine.countLabel
import com.encounterdeck.engine.formatCr
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.launch
import kotlin.math.round

private const val ATTRIBUTION =
    "This is a play aid for 5e-compatible games developed by Matthew Heusser (matt@xndev.com). " +
        "Not affiliated with, endorsed by, or sponsored by any game publisher."

// CC-BY-4.0 requires this notice to travel with the distributed work, so it
// ships in the app rather than only in the repository README. Section
// 3(a)(1)(B) also requires saying that the material was modified -- and it was:
// gen_seed.py abridges the stat blocks, reconstructs hit dice from average hit
// points, and adds environment tags that are not SRD content at all.
private const val SRD_ATTRIBUTION =
    "This work includes material from the System Reference Document 5.1 (\"SRD 5.1\") " +
        "by Wizards of the Coast LLC, available under the Creative Commons Attribution " +
        "4.0 International License (https://creativecommons.org/licenses/by/4.0/legalcode). " +
        "Modified from the original: stat blocks are abridged, hit dice are reconstructed " +
        "from average hit points, and environment tags are ours."

private val DIFFICULTIES = listOf("explorer", "balanced", "tactician", "honour")
private val LOCATIONS = listOf("any", "castle", "dungeon", "woods", "trail", "mountains", "water", "north", "desert")
private val TYPES = listOf("wandering", "big bad")

/**
 * The bundled drawing for [monster], tinted to the theme.
 *
 * The art ships as a transparent alpha mask rather than a picture, so tinting
 * is what gives it a colour at all. Renders nothing when no drawing is bundled
 * -- roughly half the monsters have none, and a repeated placeholder would say
 * less than the text that is already there.
 */
@Composable
private fun MonsterArt(monster: Monster, modifier: Modifier, tint: Color) {
    val art = artFor(monster) ?: return
    Image(
        painter = painterResource(art),
        contentDescription = null,   // the monster's name is always adjacent
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tint),
    )
}

/**
 * One-line licence strip. Required on every screen showing SRD-derived content.
 *
 * CC-BY-4.0 3(a)(2) lets the required notice live behind a link rather than
 * beside the work, so the full text sits in [AboutDialog] and this names the
 * source, the licence and the fact it was modified. Six lines of legal text on
 * a phone left the encounter card a strip; this is one line.
 */
@Composable
private fun SrdNotice(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        "SRD 5.1 · CC BY 4.0 · modified · About",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.clickable(onClick = onOpen).padding(vertical = 4.dp, horizontal = 8.dp),
    )
}

/** The full attribution text, which the one-line strip stands in for. */
@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("About EncounterDeck") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(ATTRIBUTION, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Text(SRD_ATTRIBUTION, style = MaterialTheme.typography.bodySmall)
            }
        },
    )
}

/** The app's top-level destinations. */
private enum class Tab(val label: String) { ENCOUNTER("Encounter"), BESTIARY("Bestiary") }

@Composable
fun App() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        var tab by remember { mutableStateOf(Tab.ENCOUNTER) }
        var showAbout by remember { mutableStateOf(false) }
        Surface(Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = tab == t,
                                onClick = { tab = t },
                                // Icon-less bar: the label is the only affordance,
                                // so it always shows rather than only when selected.
                                icon = {},
                                label = { Text(t.label) },
                                alwaysShowLabel = true,
                            )
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    when (tab) {
                        Tab.ENCOUNTER -> EncounterScreen { showAbout = true }
                        Tab.BESTIARY -> BestiaryScreen { showAbout = true }
                    }

                    // Nothing read showAbout for three releases: every SrdNotice
                    // set it, and no one rendered the dialog, so the licence text
                    // the strip stands in for could not be reached at all. That
                    // is a compliance problem as much as a UI one -- CC-BY-4.0
                    // 3(a)(2) lets the notice live behind a link, but only a link
                    // that opens.
                    if (showAbout) {
                        AboutDialog(onDismiss = { showAbout = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun EncounterScreen(onAbout: () -> Unit) {
    val generator = remember { EncounterGenerator(InMemoryMonsterRepository()) }
    val scope = rememberCoroutineScope()

    var level by remember { mutableStateOf(3) }
    var players by remember { mutableStateOf(4) }
    var difficulty by remember { mutableStateOf("balanced") }
    var location by remember { mutableStateOf("any") }
    var type by remember { mutableStateOf("wandering") }

    var card by remember { mutableStateOf<EncounterCard?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }

    fun generate() {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            rotation.animateTo(90f, tween(220))     // turn the current card edge-on
            try {
                val diff = Difficulty.valueOf(difficulty.uppercase())
                val loc = if (location == "any") null else Location.valueOf(location.uppercase())
                val encType = if (type == "big bad") EncounterType.BIG_BAD else EncounterType.WANDERING
                card = generator.generate(
                    EncounterRequest(level, players, diff, encType, loc)
                )
            } catch (e: Exception) {
                error = e.message ?: "Could not generate an encounter"
                card = null
            }
            rotation.animateTo(0f, tween(220))       // turn the new card into view
            busy = false
        }
    }

    // Cap the content width so a 13-inch iPad shows a readable centred column
    // rather than controls stretched across the full screen.
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
        Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("EncounterDeck", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledDropdown("Level", (1..10).toList(), level) { level = it }
            LabeledDropdown("Players", (1..8).toList(), players) { players = it }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledDropdown("Difficulty", DIFFICULTIES, difficulty) { difficulty = it }
            LabeledDropdown("Location", LOCATIONS, location) { location = it }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledDropdown("Type", TYPES, type) { type = it }
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = { generate() }, enabled = !busy) {
            Text(if (busy) "Generating…" else "Generate")
        }

        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer {
                    rotationY = rotation.value
                    cameraDistance = 16f * density
                },
            contentAlignment = Alignment.Center,
        ) {
            CardFace(card, error)
        }

        Spacer(Modifier.height(4.dp))
        SrdNotice(onAbout)
    }
    }
}

@Composable
private fun <T> LabeledDropdown(label: String, options: List<T>, selected: T, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    // Label beside the control rather than above it: two rows of these used to
    // cost four lines of vertical space, and the card is what people came for.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.width(6.dp))
        Box {
            // The visible label sits in a sibling Text, so on its own this
            // button announces just its value ("3") with no idea what it sets.
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.semantics {
                    contentDescription = "$label: $selected. Double tap to change."
                },
            ) {
                Text(selected.toString().replaceFirstChar { it.uppercase() })
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.toString().replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CardFace(card: EncounterCard?, error: String?) {
    Surface(
        Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
    ) {
        Box(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), contentAlignment = Alignment.Center) {
            when {
                error != null -> Text(
                    "⚠️  $error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
                card == null -> Text(
                    "Press Generate to draw an encounter",
                    style = MaterialTheme.typography.bodyLarge,
                )
                else -> CardContent(card)
            }
        }
    }
}

@Composable
private fun CardContent(card: EncounterCard) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // A solo Big Bad gets the room; a mixed group shares the card, so the
        // art shrinks rather than pushing the stats off the bottom.
        val artHeight = if (card.groups.size == 1) 75.dp else 52.dp

        card.groups.forEach { group ->
            val m = group.monster
            if (artFor(m) != null) {
                MonsterArt(
                    m,
                    Modifier.fillMaxWidth().height(artHeight),
                    MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(
                countLabel(m.name, group.count),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "CR ${formatCr(m.cr)}   •   AC ${m.ac}   •   ${m.size} ${m.type}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "HP (${m.hitDice}):  ${group.hitPoints.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium,
            )
            m.armor?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Armor: $it (lootable)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            immunitiesLine(group)?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center,
                )
            }

            if (m.attacks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                m.attacks.forEach { attack ->
                    Text(
                        "• $attack",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(Modifier.height(10.dp))
        Text("XP  ${card.totalXp}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Treasure  ${formatTreasure(card.treasure)}",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        if (card.specialLoot.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            card.specialLoot.forEach { item ->
                Text(
                    "✦ $item",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Power ${formatPower(card.power)}  •  party ${card.partyLevel}, " +
                "${card.numPlayers} players, ${card.difficulty.name.lowercase()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun immunitiesLine(group: MonsterGroup): String? {
    val m = group.monster
    val parts = buildList {
        if (m.damageImmunities.isNotEmpty()) add("Immune (damage): ${m.damageImmunities.joinToString(", ")}")
        if (m.conditionImmunities.isNotEmpty()) add("Immune (condition): ${m.conditionImmunities.joinToString(", ")}")
    }
    return if (parts.isEmpty()) null else parts.joinToString("\n")
}

private fun formatPower(power: Double): String {
    val rounded = round(power * 100) / 100.0
    return rounded.toString()
}

private fun formatTreasure(t: Treasure): String {
    val parts = buildList {
        if (t.pp > 0) add("${t.pp} pp")
        if (t.gp > 0) add("${t.gp} gp")
        if (t.ep > 0) add("${t.ep} ep")
        if (t.sp > 0) add("${t.sp} sp")
        if (t.cp > 0) add("${t.cp} cp")
    }
    return if (parts.isEmpty()) "none" else parts.joinToString(", ")
}

// ---------------------------------------------------------------- bestiary --

@Composable
private fun BestiaryScreen(onAbout: () -> Unit) {
    val search = remember { MonsterSearch() }

    var query by remember { mutableStateOf("") }
    var scope by remember { mutableStateOf(SearchScope.EVERYTHING) }
    var selected by remember { mutableStateOf<Monster?>(null) }
    // Suggestions drop away once a result is opened, and while browsing results.
    var showSuggestions by remember { mutableStateOf(false) }

    val results = remember(query, scope) { search.search(query, scope) }
    val suggestions = remember(query) { search.suggest(query) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val monster = selected
            if (monster != null) {
                MonsterDetail(monster) { selected = null }
                SrdNotice(onAbout)
                return@Column
            }

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    showSuggestions = true
                },
                singleLine = true,
                label = { Text("Search monsters") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchScope.entries.forEach { s ->
                    FilterChip(
                        selected = scope == s,
                        onClick = { scope = s },
                        label = { Text(if (s == SearchScope.NAME) "Name" else "Everything") },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Type-ahead: tapping a suggestion opens that monster directly.
            if (showSuggestions && suggestions.isNotEmpty() && query.isNotBlank()) {
                Surface(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp,
                ) {
                    Column {
                        suggestions.forEach { m ->
                            Text(
                                m.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = m
                                        showSuggestions = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Text(
                when {
                    query.isBlank() -> "${results.size} monsters"
                    results.isEmpty() -> "No monsters match \"$query\""
                    else -> "${results.size} ${if (results.size == 1) "match" else "matches"}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))

            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(results, key = { it.id }) { m ->
                    MonsterRow(m) {
                        selected = m
                        showSuggestions = false
                    }
                    HorizontalDivider()
                }
            }

            SrdNotice(onAbout)
        }
    }
}

@Composable
private fun MonsterRow(m: Monster, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A fixed-width slot either way, so names line up whether or not this
        // monster has art bundled.
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            MonsterArt(m, Modifier.fillMaxSize(), MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(m.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(
                "CR ${formatCr(m.cr)}  •  ${m.size} ${m.type}  •  AC ${m.ac}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ColumnScope.MonsterDetail(m: Monster, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onBack) { Text("‹ Back") }
    }
    Spacer(Modifier.height(10.dp))

    Surface(
        Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (artFor(m) != null) {
                MonsterArt(
                    m,
                    Modifier.fillMaxWidth().height(180.dp),
                    MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
            }

            Text(
                m.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "CR ${formatCr(m.cr)}   •   ${m.xp} XP",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text("${m.size} ${m.type}", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text("AC ${m.ac}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                "HP ${m.hitDice.average}  (${m.hitDice})",
                style = MaterialTheme.typography.bodyMedium,
            )

            m.armor?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Armor: $it (lootable)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (m.damageImmunities.isNotEmpty() || m.conditionImmunities.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                if (m.damageImmunities.isNotEmpty()) {
                    Text(
                        "Immune (damage): ${m.damageImmunities.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center,
                    )
                }
                if (m.conditionImmunities.isNotEmpty()) {
                    Text(
                        "Immune (condition): ${m.conditionImmunities.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (m.attacks.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Text("Attacks", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                m.attacks.forEach { attack ->
                    Text(
                        "• $attack",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    )
                }
            }

            if (m.locations.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Found in: ${m.locations.map { it.name.lowercase() }.sorted().joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}
