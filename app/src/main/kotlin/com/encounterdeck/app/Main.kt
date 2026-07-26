package com.encounterdeck.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "EncounterDeck") {
        MaterialTheme(colorScheme = darkColorScheme()) {
            App()
        }
    }
}

@Composable
private fun App() {
    val api = remember { ApiClient() }
    val scope = rememberCoroutineScope()

    var level by remember { mutableStateOf(3) }
    var players by remember { mutableStateOf(4) }
    var difficulty by remember { mutableStateOf("balanced") }

    var card by remember { mutableStateOf<CardResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }

    fun generate() {
        if (loading) return
        scope.launch {
            loading = true
            error = null
            rotation.animateTo(90f, tween(220))   // turn the current card edge-on
            try {
                card = api.generate(level, players, difficulty, "wandering")
            } catch (e: Exception) {
                error = e.message ?: "Could not reach the backend at localhost:8080"
                card = null
            }
            rotation.animateTo(0f, tween(220))     // turn the new card into view
            loading = false
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("EncounterDeck", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Dropdown("Party level", (1..10).toList(), level) { level = it }
                Dropdown("Players", (1..8).toList(), players) { players = it }
                Dropdown(
                    "Difficulty",
                    listOf("explorer", "balanced", "tactician", "honour"),
                    difficulty,
                ) { difficulty = it }
            }

            Spacer(Modifier.height(20.dp))
            Button(onClick = { generate() }, enabled = !loading) {
                Text(if (loading) "Generating…" else "Generate")
            }

            Spacer(Modifier.height(28.dp))
            Box(
                Modifier
                    .width(380.dp)
                    .height(440.dp)
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
}

@Composable
private fun <T> Dropdown(label: String, options: List<T>, selected: T, onSelect: (T) -> Unit) {
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
private fun CardFace(card: CardResponse?, error: String?) {
    Surface(
        Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
    ) {
        Box(Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
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
private fun CardContent(card: CardResponse) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            card.type.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        card.monsters.forEach { m ->
            Text(
                "${m.count} × ${m.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "CR ${m.cr}   •   AC ${m.ac}   •   HP ${m.hp} each",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))
        Text("XP  ${card.totalXp}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text("Treasure  ${formatTreasure(card.treasure)}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        Text(
            "Power ${card.power}  •  party ${card.partyLevel}, ${card.numPlayers} players, ${card.difficulty}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatTreasure(t: TreasureResp): String {
    val parts = buildList {
        if (t.pp > 0) add("${t.pp} pp")
        if (t.gp > 0) add("${t.gp} gp")
        if (t.ep > 0) add("${t.ep} ep")
        if (t.sp > 0) add("${t.sp} sp")
        if (t.cp > 0) add("${t.cp} cp")
    }
    return if (parts.isEmpty()) "none" else parts.joinToString(", ")
}
