# EncounterDeck

A cross-platform (iOS + Android) app that generates random tabletop encounters
(**5e and 5.5e compatible**) and presents them as flippable cards — pick your
party level, party size, difficulty,
and encounter type, hit **Generate**, and get a scaled encounter with stats, XP,
and treasure.

It is a play aid for 5e-compatible games.

**Author:** Matthew Heusser — matt@xndev.com
Written by Matthew Heusser with help from Claude Code.

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
├── engine/                 # Kotlin Multiplatform library (JVM + Android + iOS)
│   └── src/commonMain/…    #   generate() + domain model  ← reused everywhere
├── backend/                # Ktor: GET /generate → JSON (optional; future cloud sync)
├── app/                    # Compose Multiplatform application
│   └── src/
│       ├── commonMain/…    #   shared Compose UI; calls the engine in-process
│       ├── androidMain/…   #   MainActivity + AndroidManifest
│       ├── iosMain/…       #   MainViewController (framework entry)
│       └── desktopMain/…   #   desktop window entry
└── iosApp/                 # Xcode project (SwiftUI host + xcodegen project.yml)
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

## Running

Requires JDK 21. Android needs the Android SDK; iOS needs Xcode (macOS only).
The Compose app calls the engine **in-process**, so no backend has to be running.

**Android** — open the `EncounterDeck` folder in Android Studio, let it sync, then
Run the `app` configuration on an emulator or device. From the CLI:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**iOS** (macOS + Xcode) — open `iosApp/iosApp.xcodeproj` in Xcode and Run on a
simulator. The build compiles the shared Kotlin framework automatically. The
project is generated by [xcodegen](https://github.com/yonaskolb/XcodeGen) from
`iosApp/project.yml`; if you change the spec, run `xcodegen generate` in `iosApp/`.

**Desktop** (JVM):

```bash
./gradlew :app:run
```

**Backend API** (optional — not used by the app; kept for future cloud sync):

```bash
./gradlew :backend:run
curl "http://localhost:8080/generate?partyLevel=3&numPlayers=4&difficulty=tactician&location=dungeon"
```

**Tests**:

```bash
./gradlew :engine:jvmTest :backend:test
```

## Status

**Phases 0–2 complete** — one shared Kotlin engine, three front ends, verified running:

- ✅ `engine/` — Kotlin Multiplatform library (JVM + Android + iOS); generator,
  ~34-monster SRD seed with immunities/attacks/locations, 18 unit tests
- ✅ `backend/` — Ktor `GET /generate` → JSON, 5 endpoint tests
- ✅ `app/` — Compose Multiplatform: **Android + iOS + desktop** from one UI,
  dropdowns (level / players / difficulty / location) + card-flip, engine in-process
- ✅ Verified running on the Android emulator and the iOS Simulator

Next: on-device persistence (SQLDelight behind the repository interface), more
encounter types (traps / big-bad), then cloud sync.

## Game content (SRD 5.1)

This app uses **only open game content from the System Reference Document 5.1
(SRD 5.1)**. It contains no Product Identity and no closed/edition-specific
content, and it is not affiliated with, endorsed by, or sponsored by any game
publisher. It is an independent play aid for 5e-compatible games.

> This work includes material from the System Reference Document 5.1 ("SRD 5.1")
> by Wizards of the Coast LLC, available under the Creative Commons Attribution
> 4.0 International License
> (<https://creativecommons.org/licenses/by/4.0/legalcode>). Modified from the
> original: stat blocks are abridged, hit dice are reconstructed from average
> hit points, and environment tags are ours.

Monster data was imported from the community SRD dataset at
<https://github.com/5e-bits/5e-database> (also SRD 5.1 under CC-BY-4.0). The
"modified" sentence is not decoration -- CC-BY-4.0 section 3(a)(1)(B) requires
indicating that the material was changed, and `scripts/gen_seed.py` does change
it. The notice ships on every screen that shows SRD-derived content.

Bundled artwork is *not* SRD content and is separately public domain or CC0;
see [ART.md](ART.md).

Note: SRD content (used here under CC-BY-4.0) is distinct from the
[PolyForm Noncommercial](LICENSE) terms below, which cover **this project's own
code**.

## License

Copyright © 2026 Matthew Heusser.

**For everyone else** — released under the
[PolyForm Noncommercial License 1.0.0](LICENSE): use, run, modify, and share it
for any **noncommercial** purpose, and contributions are welcome. Commercial use,
including selling it, is not permitted for third parties.

**Author's commercial rights** — as the copyright holder, Matthew Heusser retains
all commercial rights and may sell or license this software (e.g., as a paid app).
A license grants rights to others; it does not limit the owner.

**Contributing** — by contributing you agree to the
[Contributor License Agreement](CONTRIBUTING.md), which lets your contribution be
shared publicly under PolyForm Noncommercial **and** included in the author's
commercial versions. This is what keeps the app sellable.

*(Bundled SRD 5.1 content is CC-BY-4.0, which permits commercial use with the
attribution shown above.)*
