package com.encounterdeck.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.encounterdeck.engine.Difficulty
import com.encounterdeck.engine.EncounterCard
import com.encounterdeck.engine.EncounterGenerator
import com.encounterdeck.engine.EncounterRequest
import com.encounterdeck.engine.EncounterType
import com.encounterdeck.engine.InMemoryMonsterRepository
import com.encounterdeck.engine.Location
import com.encounterdeck.engine.MonsterGroup
import com.encounterdeck.engine.Treasure
import com.encounterdeck.engine.countLabel
import kotlinx.coroutines.launch
import kotlin.math.round

private const val ATTRIBUTION =
    "This is a play aid for 5e-compatible games developed by Matthew Heusser (matt@xndev.com)."

private val DIFFICULTIES = listOf("explorer", "balanced", "tactician", "honour")
private val LOCATIONS = listOf("any", "castle", "dungeon", "woods", "trail", "mountains", "water", "north", "desert")
private val TYPES = listOf("wandering", "big bad")

@Composable
fun App() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            EncounterScreen()
        }
    }
}

@Composable
private fun EncounterScreen() {
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

    Column(
        Modifier.fillMaxSize().safeContentPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("EncounterDeck", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            ATTRIBUTION,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledDropdown("Level", (1..10).toList(), level) { level = it }
            LabeledDropdown("Players", (1..8).toList(), players) { players = it }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledDropdown("Difficulty", DIFFICULTIES, difficulty) { difficulty = it }
            LabeledDropdown("Location", LOCATIONS, location) { location = it }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledDropdown("Type", TYPES, type) { type = it }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = { generate() }, enabled = !busy) {
            Text(if (busy) "Generating…" else "Generate")
        }

        Spacer(Modifier.height(20.dp))
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
    }
}

@Composable
private fun <T> LabeledDropdown(label: String, options: List<T>, selected: T, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }) {
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
        Box(Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
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
        card.groups.forEach { group ->
            val m = group.monster
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

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
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
        Spacer(Modifier.height(16.dp))
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

private fun formatCr(cr: Double): String = when (cr) {
    0.125 -> "1/8"
    0.25 -> "1/4"
    0.5 -> "1/2"
    else -> cr.toInt().toString()
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
