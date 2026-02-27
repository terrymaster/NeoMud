# Balance Audit Findings (Feb 2026)

## Critical Bugs Found
1. Skill classRestriction mismatches: BASH missing CLERIC/WARLOCK, SNEAK missing MISSIONARY, MEDITATE missing PALADIN/MISSIONARY/BARD/GYPSY, KICK missing NINJA, PICK_LOCK includes NINJA but Ninja class lacks it
2. Magic school level number (1/2/3 in classes.json) is NEVER enforced — all classes with any school access can cast ALL spells of that school. Ranger casts Nature's Wrath, Gypsy casts Fireball.
3. DoT/HoT tick application to NPCs needs verification — EffectApplicator uses Player object, may not apply to NPCs

## Class Viability (Solo, L1)
- S: Warrior (one-shots wolves, tanky, 1.0x XP)
- A: WitchHunter, Ranger, Ninja, Thief
- B: Mage, Druid, Warlock, Mystic
- C: Cleric, Paladin, Bard, Gypsy, Missionary
- F: Priest (NO damage spells, worst HP, 1.2x XP)

## Key Math Results
- L1 Warrior one-shots Shadow Wolf (47.5 avg damage vs 30 HP)
- Melee DPS is 3-5x higher than spell DPS
- Full leather armor (10 value) reduces wolf damage to minimum (1)
- Sneak DC 15 never fails for any sneaker class (AGI 20+ guaranteed success)
- Pick Lock DCs never fail for Thief (AGI 30 + INT 12 + d20 always beats DC 16)
- Parry/Dodge proc rates are 5-6% at typical L1 stats — too low to notice
- Soothing Song HoT may apply full power per tick for 6 ticks (overtuned if so)
- XP becomes negligible at 5+ levels above content (10% modifier)

## Content Gaps
- Large difficulty jump between Forest (L1-3, max 5dmg) and Marsh (L4-5, 15-25dmg)
- No L3-4 transitional content bridging forest → marsh
- Dead-end rooms (cave, ruins, stream) have zero reward in forest
- No quests, no boss encounters, no group incentives
- Crafting materials (pelts, fangs, marsh hide, wraith essence, obsidian shard) have no crafting system

## Economy
- Full L1 gear costs ~185 copper, ~10 wolf kills to afford
- Full L3 gear costs ~510 copper, ~26 wolf kills
- L6 plate set available from Enchantress Lyra in town
- No money sinks after gear purchase
- Crafting materials drop but have no use yet

## Zone Progression Summary
| Zone | Level | NPCs | HP Range | Dmg Range | Key Drops |
|------|-------|-------|----------|-----------|-----------|
| Forest | 1-3 | 4 types | 15-30 | 2-5 | Wolf Pelt, Spider Fang, basic gear |
| Marsh | 4-5 | 4 types | 80-180 | 15-25 | Marsh Hide, Wraith Essence |
| Gorge | 6-7 | 4 types | 130-220 | 26-32 | Obsidian Shard, high-tier gear |
