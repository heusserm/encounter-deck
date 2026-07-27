import json, re, os, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
URL = "https://raw.githubusercontent.com/5e-bits/5e-database/main/src/2014/en/5e-SRD-Monsters.json"
SRC = os.path.join(ROOT, "scripts", "monsters.json")
if not os.path.exists(SRC):
    urllib.request.urlretrieve(URL, SRC)
OUT = os.path.join(ROOT, "engine", "src", "commonMain", "kotlin", "com", "encounterdeck", "engine", "SeedMonsters.kt")

data = json.load(open(SRC))

def esc(s):
    return s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$")

def kstr(s):
    return '"' + esc(s) + '"'

def klist(items):
    return "emptyList()" if not items else "listOf(" + ", ".join(kstr(i) for i in items) + ")"

def parse_hit_dice(hd, hp):
    m = re.match(r"(\d+)d(\d+)", hd or "")
    if not m:
        return (1, 8, max(1, hp - 4))
    count, die = int(m.group(1)), int(m.group(2))
    return (count, die, hp - (count * (die + 1)) // 2)

def armor_desc(m):
    worn = []
    for ac in m.get("armor_class", []):
        if ac.get("type") == "armor":
            worn += [a["name"].lower() for a in ac.get("armor", [])]
        elif ac.get("type") == "shield":
            worn.append("shield")
    seen, out = set(), []
    for w in worn:
        if w not in seen:
            seen.add(w); out.append(w)
    return ", ".join(out) if out else None

def first_ac(m):
    acs = m.get("armor_class", [])
    return acs[0]["value"] if acs else 10

def attacks_for(m):
    out = []
    for a in m.get("actions", []):
        name = a.get("name", "")
        if name.lower().startswith("multiattack"):
            desc = (a.get("desc") or "").split(". ")[0].strip().rstrip(".")
            out.append(desc if desc and len(desc) <= 100 else "Multiattack")
            continue
        dmgs = []
        for dd in a.get("damage", []):
            if isinstance(dd, dict) and dd.get("damage_dice"):
                dt = (dd.get("damage_type") or {}).get("name", "")
                dmgs.append(f"{dd['damage_dice']} {dt.lower()}".strip())
        dmgstr = " + ".join(dmgs)
        if a.get("attack_bonus") is not None:
            b = a["attack_bonus"]
            sign = "+" if b >= 0 else ""
            out.append(f"{name} {sign}{b} ({dmgstr})" if dmgstr else f"{name} {sign}{b}")
        elif a.get("dc") and dmgstr:
            dc = a["dc"]
            dct = (dc.get("dc_type") or {}).get("name", "")
            out.append(f"{name} ({dmgstr}, DC {dc.get('dc_value','?')} {dct})")
        if len(out) >= 5:
            break
    return out

AQUATIC = ["shark", "octopus", "crab", "fish", "eel", "whale", "seahorse", "crocodile",
           "frog", "toad", "turtle", "hydra", "kuo-toa", "sahuagin", "merfolk", "merrow",
           "reef", "marid", "plesiosaur", "dragon turtle", "sea hag", "water elemental",
           "giant sea", "nixie", "locathah"]
ARCTIC = ["polar", "frost", "white dragon", "yeti", "mammoth", "remorhaz", "winter wolf",
          "abominable", "snowy", "ice mephit"]
DESERT = ["desert", "sand", "scorpion", "camel", "blue dragon", "mummy", "salamander",
          "dust", "brass dragon", "gorgon", " dao"]
FLY = ["eagle", "hawk", "vulture", "bat", "pteranodon", "wyvern", "griffon", "pegasus",
       "peryton", "couatl", "sprite", "pixie", "harpy", "owl", "roc", "air elemental",
       "djinni", "gargoyle"]

def locations_for(m):
    t = m.get("type", "")
    name = m.get("name", "").lower()
    def has(kws): return any(k in name for k in kws)
    aq, ar, de, fl = has(AQUATIC), has(ARCTIC), has(DESERT), has(FLY)
    special = aq or ar or de
    locs = set()
    if aq: locs.add("WATER")
    if ar: locs.update(["NORTH", "MOUNTAINS"])
    if de: locs.add("DESERT")
    if fl and not aq: locs.update(["MOUNTAINS", "TRAIL"])

    if t == "beast":
        if not special:
            locs.update(["WOODS", "TRAIL"])
            if has(["mountain", "goat", "ram"]): locs.add("MOUNTAINS")
    elif t == "dragon":
        locs.update(["MOUNTAINS", "DUNGEON"])
        if "green" in name or "black" in name: locs.update(["WOODS", "WATER"])
    elif t == "giant":
        if not special: locs.update(["MOUNTAINS", "TRAIL"])
    elif t == "humanoid":
        if not special:
            locs.update(["DUNGEON", "TRAIL"])
            if has(["guard", "knight", "noble", "cult", "priest", "mage", "captain", "veteran", "commoner", "acolyte"]): locs.add("CASTLE")
            if has(["tribal", "scout", "berserker", "druid", "goblin", "hobgoblin", "orc", "gnoll", "bugbear", "kobold"]): locs.add("WOODS")
    elif t == "undead":
        locs.update(["DUNGEON"] if special else ["DUNGEON", "CASTLE"])
    elif t == "construct":
        locs.update(["CASTLE", "DUNGEON"])
    elif t == "fiend":
        locs.update(["DUNGEON", "CASTLE"])
    elif t == "fey":
        if not special: locs.update(["WOODS"])
    elif t == "plant":
        if not special: locs.update(["WOODS", "WATER"])
    elif t == "monstrosity":
        if not special: locs.update(["MOUNTAINS", "DUNGEON", "WOODS"])
    elif t == "aberration":
        locs.update(["DUNGEON"])
    elif t == "ooze":
        locs.update(["DUNGEON"])
    elif t == "elemental":
        if not special: locs.update(["MOUNTAINS", "DUNGEON"])
    elif t == "celestial":
        locs.update(["CASTLE", "MOUNTAINS"])
    if not locs:
        locs.update(["DUNGEON", "TRAIL"])
    return sorted(locs)

rows = []
for m in sorted(data, key=lambda x: (x.get("challenge_rating", 0), x.get("name", ""))):
    cr = float(m.get("challenge_rating", 0))
    hp = int(m.get("hit_points", 1) or 1)
    cnt, die, mod = parse_hit_dice(m.get("hit_dice", ""), hp)
    armor = armor_desc(m)
    parts = [
        f'id = {kstr(m["index"])}',
        f'name = {kstr(m["name"])}',
        f'cr = {cr}',
        f'xp = {int(m.get("xp", 0) or 0)}',
        f'hitDice = HitDice({cnt}, {die}, {mod})',
        f'ac = {first_ac(m)}',
        f'size = {kstr(m.get("size", "Medium"))}',
        f'type = {kstr(m.get("type", "humanoid"))}',
        f'armor = {kstr(armor) if armor else "null"}',
        f'damageImmunities = {klist([s.lower() for s in m.get("damage_immunities", []) if s])}',
        f'conditionImmunities = {klist([c.get("name","").lower() for c in m.get("condition_immunities", []) if c.get("name")])}',
        f'attacks = {klist(attacks_for(m))}',
        f'locations = setOf({", ".join(locations_for(m))})',
    ]
    rows.append("        Monster(\n            " + ",\n            ".join(parts) + ",\n        ),")

header = '''package com.encounterdeck.engine

import com.encounterdeck.engine.Location.CASTLE
import com.encounterdeck.engine.Location.DESERT
import com.encounterdeck.engine.Location.DUNGEON
import com.encounterdeck.engine.Location.MOUNTAINS
import com.encounterdeck.engine.Location.NORTH
import com.encounterdeck.engine.Location.TRAIL
import com.encounterdeck.engine.Location.WATER
import com.encounterdeck.engine.Location.WOODS

/**
 * Monster seed auto-generated from the SRD 5.1 monster dataset
 * (github.com/5e-bits/5e-database, SRD 5.1 content under CC-BY-4.0).
 *
 * Hit-dice modifiers are reconstructed from average HP; location tags are
 * assigned heuristically by creature type and name. Regenerate with
 * scripts/gen_seed.py.
 */
object SeedMonsters {
    val ALL: List<Monster> = listOf(
'''

open(OUT, "w").write(header + "\n".join(rows) + "\n    )\n}\n")
print(f"wrote {len(rows)} monsters")
