#!/usr/bin/env python3
"""Regenerate app/src/commonMain/kotlin/com/encounterdeck/app/MonsterArt.kt.

Maps each seeded monster to a bundled drawing. Every image is public domain or
CC0 (see ART.md), so nothing here carries an attribution requirement.

Coverage is deliberately partial. A monster with no confident match is left out
and the detail screen falls back to its text layout, which is much better than
showing the wrong creature. Two rules earn their keep:

  * "Giant Badger" is a badger, not a giant. Only the true giants get the
    giant drawing; "Giant X" and "Swarm of X" resolve to X.
  * BLOCKED lists matches that a name search found and a human rejected --
    a writing script for Deva, a machine part for Planetar, a snake's rattle
    for the snakes. Keep them listed so a future regeneration cannot
    silently reintroduce them.

Usage:  python3 scripts/gen_art.py               # uses the checked-in mapping
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SEED = ROOT / "engine/src/commonMain/kotlin/com/encounterdeck/engine/SeedMonsters.kt"
OUT = ROOT / "app/src/commonMain/kotlin/com/encounterdeck/app/MonsterArt.kt"
DRAWABLES = ROOT / "app/src/commonMain/composeResources/drawable"

# Fantasy creatures: LadyofHats' CC0 "DnD" set on Wikimedia Commons.
# First match wins, so more specific names come first.
DND = [
    ("hobgoblin", "dnd_hobgoblin"), ("goblin", "dnd_goblin"),
    ("black pudding", "dnd_black_pudding"), ("ochre jelly", "dnd_ochre_jelly"),
    ("gray ooze", "dnd_gray_ooze"), ("green slime", "dnd_green_slime"),
    ("basilisk", "dnd_basilisk"), ("centaur", "dnd_centaur"),
    ("chimera", "dnd_chimera"), ("cockatrice", "dnd_cockatrice"),
    ("djinni", "dnd_djinn"), ("efreeti", "dnd_efreeti"),
    ("dragon", "dnd_dragon"), ("wyvern", "dnd_dragon"),
    ("dryad", "dnd_dryad"), ("dwarf", "dnd_dwarf"), ("elf", "dnd_elven"),
    ("gargoyle", "dnd_gargoyle"), ("ghoul", "dnd_ghoul"), ("ghast", "dnd_ghoul"),
    ("gnoll", "dnd_gnoll"), ("gnome", "dnd_gnome"), ("kobold", "dnd_kobold"),
    ("griffon", "dnd_griffon"), ("hippogriff", "dnd_hippogriff"),
    ("hydra", "dnd_hydra"), ("invisible stalker", "dnd_invisible_stalker"),
    ("werewolf", "dnd_lycantrop"), ("wererat", "dnd_lycantrop"),
    ("wereboar", "dnd_lycantrop"), ("weretiger", "dnd_lycantrop"),
    ("werebear", "dnd_lycantrop"), ("manticore", "dnd_manticore"),
    ("medusa", "dnd_medusa"), ("minotaur", "dnd_minotaur"),
    ("mummy", "dnd_mummy"), ("nixie", "dnd_nixie"), ("ogre", "dnd_ogre"),
    ("orc", "dnd_orc"), ("pegasus", "dnd_pegasus"), ("pixie", "dnd_pixie"),
    ("sprite", "dnd_pixie"), ("purple worm", "dnd_purpleworm"),
    ("roc", "dnd_roc"), ("specter", "dnd_spectre"), ("wraith", "dnd_spectre"),
    ("ghost", "dnd_spectre"), ("earth elemental", "dnd_stone_elemental"),
    ("troll", "dnd_troll"), ("unicorn", "dnd_unicorn"),
    ("vampire", "dnd_vampire"), ("wight", "dnd_wights"),
    ("skeleton", "dnd_skelleton"), ("treant", "dnd_treant"),
]

# The giant drawing is a humanoid giant. Only these are one.
TRUE_GIANTS = {"cloud giant", "fire giant", "frost giant", "hill giant", "stone giant"}

# Real animals: Pearson Scott Foresman's donated public-domain clipart.
ANIMALS = {
    "ape": "barbary_ape_psf", "baboon": "baboon_psf", "badger": "badger_psf",
    "bear": "black_bear_psf", "black bear": "black_bear_psf",
    "brown bear": "grizzly_bear_psf", "polar bear": "polar_bear_psf",
    "boar": "boar_psf", "camel": "bactrian_camel_psf", "cat": "siamese_cat_psf",
    "crab": "fiddler_crab_psf", "deer": "deer_psf", "eagle": "bald_eagle_psf",
    "elk": "elk_1_psf", "frog": "toad_psf", "goat": "goat_1_psf",
    "hawk": "hawk_psf", "blood hawk": "hawk_psf", "hyena": "hyena_psf",
    "jackal": "jackal_psf", "lion": "lion_psf", "lizard": "monitor_lizard_psf",
    "mule": "mule_psf", "octopus": "octopus_2_psf", "owl": "screech_owl_psf",
    "panther": "leopard_psf", "pony": "shetland_pony_psf", "rat": "mouse_psf",
    "raven": "raven_1_psf", "rhinoceros": "rhinoceros_psf",
    "scorpion": "scorpion_2_psf", "sea horse": "seahorse_line_art_psf_s_820005_cropped",
    "shark": "shark_psf", "reef shark": "shark_psf", "hunter shark": "shark_psf",
    "spider": "spider_psf", "tiger": "tiger_2_psf",
    "saber-toothed tiger": "tiger_2_psf", "triceratops": "triceratops_psf",
    "tyrannosaurus rex": "tyrannosaurus_psf", "vulture": "vulture_psf",
    "weasel": "weasel_psf", "wolf": "wolf_psf", "dire wolf": "wolf_psf",
    "winter wolf": "wolf_psf", "worg": "wolf_psf", "elephant": "indian_elephant_psf",
    "killer whale": "whale_psf", "plesiosaurus": "plesiosaur_psf",
    "axe beak": "ostrich_psf", "death dog": "german_shepherd_dog_line_art_psf_g_390001_cropped",
    "noble": "noble_psf", "satyr": "satyr_psf",
    "swarm of beetles": "beetle_psf", "swarm of centipedes": "centipede_psf",
    "swarm of insects": "insect_psf", "swarm of wasps": "mason_wasp_psf",
    "toad": "toad_psf", "snake": "rattlesnake_psf", "cobra": "cobra_african_psf",
    "quipper": "dory_fish_psf", "fish": "dory_fish_psf",
    "beetle": "beetle_psf", "centipede": "centipede_psf", "wasp": "mason_wasp_psf",
    "insect": "insect_psf", "bat": "dnd_lycantrop",
}

# Drawings that were downloaded and then rejected on review. The app tints the
# art to the theme colour, which flatters clean line art and ruins a shaded
# scene -- a full landscape plate turns into a grey block. Listed by file so a
# regeneration cannot quietly bring them back.
REJECTED_ART = {
    # Wrong subject
    "noble_psf": "an English gold coin, not a nobleman",
    "rattlesnake_psf": 'the word "RATTLE" is printed across it',
    "german_shepherd_dog_line_art_psf_g_390001_cropped": "carries its caption text",
    "frogs_psf": "frogging -- the braided coat fastenings, not the amphibian",
    # Scene illustrations: background shading renders as a grey slab
    "dnd_basilisk": "dark cave scene", "dnd_chimera": "unreadable when tinted",
    "dnd_purpleworm": "busy cavern scene", "dnd_wights": "dark forest scene",
    "dnd_black_pudding": "cave scene; the pudding is barely visible",
    "dnd_gray_ooze": "framed, and the ooze reads as a smear",
    "dnd_centaur": "drawn inside a filled circle",
    "dnd_djinn": "drawn inside a filled circle",
    "dnd_dryad": "framed woodland scene", "dnd_efreeti": "dark scene",
    "dnd_gargoyle": "architectural scene", "dnd_ghoul": "framed graveyard scene",
    "dnd_giant": "landscape with foliage", "dnd_gnoll": "dark scene",
    "dnd_gnome": "framed interior", "dnd_invisible_stalker": "a library interior",
    "dnd_lycantrop": "busy transformation scene", "dnd_medusa": "framed scene",
    "dnd_ochre_jelly": "dungeon scene", "dnd_pegasus": "sky scene, inked corners",
    "dnd_pixie": "busy woodland scene", "dnd_treant": "landscape; the treant is lost in it",
    "dnd_troll": "swamp scene", "dnd_unicorn": "a hunt scene with several figures",
    "dnd_vampire": "interior scene with figures",
}

# Matches a name search turned up that are the wrong subject entirely.
# Kept explicit so a regeneration cannot quietly bring them back.
BLOCKED = {
    "Deva": "Devanagari -- a writing script, not a celestial",
    "Planetar": "Planetary Gear -- a machine part",
    "Solar": "Solar System diagram",
    "Knight": "a helmet, not a knight",
    "Salamander": "the amphibian; the SRD salamander is a fire elemental",
    "Mammoth": "an Indian elephant, missing everything that makes it a mammoth",
    "Draft Horse": "an anatomy diagram", "Riding Horse": "an anatomy diagram",
    "Warhorse": "an anatomy diagram",
}

VARIANT = re.compile(r"^(giant|swarm of|dire|winter|blood|reef|hunter|saber-toothed)\s+", re.I)
# Adjectives that describe a variant, not a different creature: "Giant Fire
# Beetle" is a beetle, "Giant Wolf Spider" is a spider, "Giant Rat (Diseased)"
# is a rat. Strip them and the head noun is left.
QUALIFIER = re.compile(r"\b(fire|wolf|poisonous|constrictor|flying|diseased|"
                       r"death|winter|dire|blood|reef|hunter|giant)\b|\([^)]*\)", re.I)


def resolve(name):
    """The drawable for `name`, or None to fall back to the text layout."""
    if name in BLOCKED:
        return None
    low = name.lower()

    if low in ANIMALS:                       # exact animal first: "dire wolf"
        return ANIMALS[low]
    if low in TRUE_GIANTS:
        return "dnd_giant"

    for pat, res in DND:                     # fantasy creatures
        if pat in low:
            return res

    # "Giant Badger" is a badger, so strip the qualifiers and retry on what is
    # left. Progressively: the leading variant word, then any descriptive
    # adjectives and parentheticals.
    for candidate in (VARIANT.sub("", low).strip(),
                      re.sub(r"\s+", " ", QUALIFIER.sub("", low)).strip()):
        if not candidate or candidate == low:
            continue
        for form in (candidate, candidate[:-1] if candidate.endswith("s") else candidate):
            if form in ANIMALS:
                return ANIMALS[form]
        for pat, res in DND:
            if pat in candidate:
                return res
    return None


def main():
    src = SEED.read_text()
    ids = re.findall(r'id = "([^"]*)"', src)
    names = re.findall(r'name = "([^"]*)"', src)
    available = {p.stem for p in DRAWABLES.glob("*.webp")}

    rows, missing = [], set()
    for mid, name in zip(ids, names):
        res = resolve(name)
        if res is None:
            continue
        if res in REJECTED_ART:
            continue
        if res not in available:
            missing.add(res)
            continue
        rows.append((mid, res, name))

    if missing:
        print(f"warning: {len(missing)} drawables referenced but not bundled: "
              f"{sorted(missing)}", file=sys.stderr)

    body = "\n".join(f'    "{mid}" to Res.drawable.{res},' for mid, res, _ in rows)
    OUT.write_text(f'''package com.encounterdeck.app

import com.encounterdeck.engine.Monster
import encounterdeck.app.generated.resources.Res
import encounterdeck.app.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource

/**
 * Bundled artwork for monsters, keyed by [Monster.id].
 *
 * Every image is public domain or CC0, so none carries an attribution
 * requirement; ART.md records where each one came from.
 *
 * Coverage is partial on purpose: {len(rows)} of {len(ids)} monsters. A monster with
 * no confident match is absent and [artFor] returns null, so the detail screen
 * falls back to its text layout rather than showing the wrong creature.
 *
 * Art is shared -- every dragon uses the same drawing -- so {len(rows)} monsters
 * need only {len({r[1] for r in rows})} images.
 *
 * Generated by scripts/gen_art.py. Edit that, not this file.
 */
private val ART: Map<String, DrawableResource> = mapOf(
{body}
)

/** The bundled drawing for [monster], or null when nothing suitable is bundled. */
fun artFor(monster: Monster): DrawableResource? = ART[monster.id]
''')
    used = {r[1] for r in rows}
    print(f"{len(rows)}/{len(ids)} monsters mapped to {len(used)} drawables")
    unused = available - used
    if unused:
        print(f"{len(unused)} bundled drawables now unused: {sorted(unused)}")


if __name__ == "__main__":
    main()
