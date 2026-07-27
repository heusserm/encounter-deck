import json, re

import os, urllib.request
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
    if not items:
        return "emptyList()"
    return "listOf(" + ", ".join(kstr(i) for i in items) + ")"

def parse_hit_dice(hd, hp):
    m = re.match(r"(\d+)d(\d+)", hd or "")
    if not m:
        return (1, 8, max(1, hp - 4))
    count, die = int(m.group(1)), int(m.group(2))
    modifier = hp - (count * (die + 1)) // 2
    return (count, die, modifier)

def armor_desc(m):
    worn = []
    for ac in m.get("armor_class", []):
        if ac.get("type") == "armor":
            for a in ac.get("armor", []):
                worn.append(a["name"].lower())
        elif ac.get("type") == "shield":
            worn.append("shield")
    # de-dup preserving order
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
            if desc:
                out.append(desc if len(desc) <= 100 else name)
            else:
                out.append("Multiattack")
            continue
        dmgs = []
        for dd in a.get("damage", []):
            if isinstance(dd, dict) and dd.get("damage_dice"):
                dt = (dd.get("damage_type") or {}).get("name", "")
                dmgs.append(f"{dd['damage_dice']} {dt.lower()}".strip())
        dmgstr = " + ".join(dmgs)
        if a.get("attack_bonus") is not None:
            bonus = a["attack_bonus"]
            sign = "+" if bonus >= 0 else ""
            out.append(f"{name} {sign}{bonus} ({dmgstr})" if dmgstr else f"{name} {sign}{bonus}")
        elif a.get("dc") and dmgstr:
            dc = a["dc"]
            dct = (dc.get("dc_type") or {}).get("name", "")
            out.append(f"{name} ({dmgstr}, DC {dc.get('dc_value','?')} {dct})")
        if len(out) >= 5:
            break
    return out

AQUATIC = ["shark","octopus","crab","fish","eel","whale","seahorse","crocodile","frog",
           "toad","turtle","hydra","kuo-toa","sahuagin","merfolk","merrow","reef","marid",
           "plesiosaurus","dragon turtle","water elemental","giant sea"]
FLY = ["eagle","hawk","vulture","bat","pteranodon","wyvern","griffon","pegasus","peryton",
       "couatl","sprite","pixie","harpy","owl","roc","air elemental","djinni","gargoyle"]

def locations_for(m):
    t = m.get("type", "")
    name = m.get("name", "").lower()
    locs = set()
    if any(k in name for k in AQUATIC): locs.add("WATER")
    if any(k in name for k in FLY): locs.update(["MOUNTAINS", "TRAIL"])
    if t == "beast":
        locs.update(["WOODS", "TRAIL"])
        if any(k in name for k in ["mountain","goat","ram","eagle","hawk"]): locs.add("MOUNTAINS")
    elif t == "dragon":
        locs.update(["MOUNTAINS", "DUNGEON"])
        if "black" in name or "green" in name: locs.update(["WATER", "WOODS"])
        if "blue" in name: locs.add("TRAIL")
    elif t == "giant":
        locs.update(["MOUNTAINS", "TRAIL"])
    elif t == "humanoid":
        locs.update(["DUNGEON", "TRAIL"])
        if any(k in name for k in ["guard","knight","noble","cult","priest","mage","captain","veteran","commoner","acolyte"]): locs.add("CASTLE")
        if any(k in name for k in ["tribal","scout","berserker","druid","goblin","hobgoblin","orc","gnoll"]): locs.add("WOODS")
    elif t == "undead":
        locs.update(["DUNGEON", "CASTLE"])
    elif t == "construct":
        locs.update(["CASTLE", "DUNGEON"])
    elif t == "fiend":
        locs.update(["DUNGEON", "CASTLE"])
    elif t == "fey":
        locs.update(["WOODS"])
    elif t == "plant":
        locs.update(["WOODS", "WATER"])
    elif t == "monstrosity":
        locs.update(["MOUNTAINS", "DUNGEON", "WOODS"])
    elif t == "aberration":
        locs.update(["DUNGEON"])
    elif t == "ooze":
        locs.update(["DUNGEON"])
    elif t == "elemental":
        locs.update(["MOUNTAINS", "DUNGEON"])
    elif t == "celestial":
        locs.update(["CASTLE", "MOUNTAINS"])
    if not locs:
        locs.update(["DUNGEON", "TRAIL"])
    return sorted(locs)

rows = []
for m in sorted(data, key=lambda x: (x.get("challenge_rating", 0), x.get("name", ""))):
    cr = float(m.get("challenge_rating", 0))
    xp = int(m.get("xp", 0) or 0)
    hp = int(m.get("hit_points", 1) or 1)
    cnt, die, mod = parse_hit_dice(m.get("hit_dice", ""), hp)
    ac = first_ac(m)
    size = m.get("size", "Medium")
    typ = m.get("type", "humanoid")
    armor = armor_desc(m)
    dmg_imm = [s.lower() for s in m.get("damage_immunities", []) if s]
    cond_imm = [c.get("name", "").lower() for c in m.get("condition_immunities", []) if c.get("name")]
    attacks = attacks_for(m)
    locs = locations_for(m)

    armor_kt = kstr(armor) if armor else "null"
    parts = [
        f'id = {kstr(m["index"])}',
        f'name = {kstr(m["name"])}',
        f'cr = {cr}',
        f'xp = {xp}',
        f'hitDice = HitDice({cnt}, {die}, {mod})',
        f'ac = {ac}',
        f'size = {kstr(size)}',
        f'type = {kstr(typ)}',
        f'armor = {armor_kt}',
        f'damageImmunities = {klist(dmg_imm)}',
        f'conditionImmunities = {klist(cond_imm)}',
        f'attacks = {klist(attacks)}',
        f'locations = setOf({", ".join(locs)})',
    ]
    rows.append("        Monster(\n            " + ",\n            ".join(parts) + ",\n        ),")

header = '''package com.encounterdeck.engine

import com.encounterdeck.engine.Location.CASTLE
import com.encounterdeck.engine.Location.DUNGEON
import com.encounterdeck.engine.Location.MOUNTAINS
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
footer = "\n    )\n}\n"

open(OUT, "w").write(header + "\n".join(rows) + footer)
print(f"wrote {len(rows)} monsters to SeedMonsters.kt")
# distribution
from collections import Counter
crc = Counter(float(m.get("challenge_rating",0)) for m in data)
print("CR distribution (fractional..9):", {k:crc[k] for k in sorted(crc) if k<=9})
