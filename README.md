# EncounterDeck

A cross-platform (iOS + Android) app that generates random tabletop encounters
(**5e and 5.5e compatible**) and presents them as flippable cards — pick your
party level, party size, difficulty,
and encounter type, hit **Generate**, and get a scaled encounter with stats, XP,
and treasure.

---

## Decisions (locked)

| Area | Decision |
|---|---|
| **Ruleset** | 5e and 5.5e compatible — SRD content only (safe to ship) |
| **Data** | Bundled read-only local DB, behind a repository interface; cloud sync later is a swap, not a rewrite |
| **Generation** | Procedural — encounters are built from a monster pool by CR (no hand-authored encounter templates) |
| **Eventual app** | Kotlin Multiplatform + Compose (iOS + Android) |
| **POC** | Ktor backend + Compose app frontend (optional throwaway web page to eyeball the JSON) |
| **Build order** | POC backend → POC frontend → real app |

---

## The generation engine (the heart of it)

```
Inputs: partyLevel, numPlayers, difficulty, type = wandering

1. difficultyRoll = uniform random in the tier's range:
     Explorer 0.5–1.0 | Balanced 0.8–1.1 | Tactician 0.9–1.2 | Honour 1.1–1.3
2. Power        = partyLevel × numPlayers × difficultyRoll / 4
3. targetCR     = max(fractional-CR pool, partyLevel − 1)   // L1 → stirge / blood hawk / vine blight tier
4. select monster(s) at CR ≈ targetCR from the pool
5. numMonsters  = round( Power / monster.CR ),  min 1        // half-up rounding
6. XP           = Σ (monster.xp × count)                     // SRD values
7. treasure     = Σ SRD Individual-Treasure roll, once per monster (by CR bracket)
8. HP           = SRD "by the book", full
→ EncounterCard { monster(s), count, stats, XP, treasure }
```

**Design notes**

- `Power / (4 × CR)` normalizes for the fact that a CR-X monster is calibrated as a
  fair challenge for **four** level-X characters — so counts stay balanced rather than deadly.
- This naturally produces satisfying low-level swarms (e.g. ~8 stirges for a level-1 party)
  while settling toward 1–3 tougher monsters at higher levels.
- The engine is **pure Kotlin, framework-free, and heavily unit-tested**, then reused
  as-is by both the Ktor backend and the Compose app — nothing gets rewritten.

**Known tuning knobs for later** (not POC concerns):

- Mid/high-level parties tend to flatten to a single monster; bias selection toward CR
  below `partyLevel − 1`, or add a "spread" that splits the budget across 2–3 monster types.
- HP-shave on large swarms — deferred; monsters use full SRD HP for now.

---

## Data model (procedural — monsters are central)

- `monsters` — id, name, CR, XP, HP, AC, size, type, environment tags
- `treasure_by_cr` — SRD individual-treasure formulas per CR bracket (0–4, 5–10, 11–16, 17+)
- Difficulty ranges live as **code constants**, not in the DB
- `environments` / `rooms` — added at Phase 4 (maps)

---

## Directory layout

```
EncounterDeck/
├── README.md
├── engine/        # pure Kotlin: generate() + domain model + tests  ← reused everywhere
├── backend/       # Ktor: GET /generate → EncounterCard JSON
├── data/          # SQLite seed DB + SRD seed content
├── app/           # Compose Multiplatform (added Phase 2; POC may start minimal here)
└── frontend-poc/  # optional throwaway web page
```

---

## Roadmap

- **Phase 0 — POC:** engine (wandering only) + small SRD monster seed spanning a CR range
  + Ktor `/generate` + Compose screen (dropdowns → Generate → **card flip** → shows
  monster / stats / XP / treasure).
- **Phase 1:** all four tiers polished, full monster seed, complete XP/treasure, thorough
  tests, environment-tag filtering.
- **Phase 2:** full Compose app; local SQLite via SQLDelight behind the repository interface.
- **Phase 3:** traps + big-bad encounter types; scroll-between-categories.
- **Phase 4:** environments with maps / rooms.
- **Phase 5:** cloud sync (Ktor → cloud, repository swaps to a remote source).

---

## Status

Scoping complete. Phase 0 not yet started.
