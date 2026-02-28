# Game Designer Agent Memory

## Key File Locations
- Data files: `maker/default_world_src/world/` (classes.json, races.json, items.json, skills.json, spells.json, loot_tables.json)
- Zone files: `maker/default_world_src/world/*.zone.json` (town, forest, marsh, gorge)
- GameConfig: `server/src/main/kotlin/com/neomud/server/game/GameConfig.kt`
- Combat: `server/.../game/combat/CombatManager.kt`, `CombatUtils.kt`
- XP: `server/.../game/progression/XpCalculator.kt`
- Stats: `server/.../game/progression/StatThresholds.kt`, `CpAllocator.kt`
- Skills: `server/.../game/skills/SkillCheck.kt`
- Spells: `server/.../game/commands/SpellCommand.kt`
- Stealth: `server/.../game/StealthUtils.kt`
- Meditation: `server/.../game/MeditationUtils.kt`
- Player creation: `server/.../persistence/repository/PlayerRepository.kt`
- Stat allocation (shared): `shared/.../model/StatAllocator.kt`

## Core Formula Summary
- HP at L1: hpPerLevelMax + (health/10)*4 + threshold bonuses
- MP at L1: mpPerLevelMax + (willpower/10)*2 (if class has MP)
- Melee damage: STR + weaponBonus + thresholdBonus + random(1..weaponRange)
- Spell power: basePower + primaryStat/3 + level/2 + random(1..6)
- Hit chance: 50 + (accuracy - defense), clamped 5-95
- Player accuracy: (STR+AGI)/2 + thresholdHit + level*2 + weaponBonus
- NPC accuracy: npc.accuracy + npc.level*2
- Player defense: AGI/2 + armor/2 + level + shieldBonus
- Dodge: AGI/100 * 0.25 (max 25% at AGI 100) — GameConfig values updated from earlier 0.15
- Parry: STR/100 * 0.25 (max 25% at STR 100) — GameConfig values updated from earlier 0.15
- Death penalty: 5% XP loss (DEATH_XP_LOSS_PERCENT = 0.05, not 10%)
- XP curve: 100 * level^2.2 (exponent 2.2)
- Creation CP: 60 points, cost 1/2/3 at 0-9/10-19/20+ above min
- Level-up CP: 10 (L1-10), 15 (L11-20), 20 (L21-30)
- Meditation: 2 + willpower/10 MP per tick

## Current Content Scope (Feb 2026)
- 6 races, 15 classes, 5 magic schools (mage/priest/druid/kai/bard)
- 4 zones: Town (7 rooms, safe hub), Forest (7 rooms, L1-3), Marsh (6 rooms, L4-5), Gorge (5 rooms, L6-7)
- 41 items: 7 weapons (L1-6), leather/chain/plate armor sets, consumables, scrolls, 5 crafting mats
- 12 loot tables across all hostile NPC types
- 3 vendors in town (Barkeep, Blacksmith, Enchantress), 1 trainer (Guildmaster)

## NPC Difficulty Tiers
- **Forest (L1-3)**: Forest Rat (L1, 15HP, 2dmg), Forest Bandit (L1, 20HP, 3dmg), Shadow Wolf (L2, 30HP, 3dmg), Forest Spider (L3, 20HP, 5dmg)
- **Marsh (L4-5)**: Bog Toad (L4, 80HP, 15dmg), Bog Lurker (L4, 95HP, 18dmg), Marsh Wraith (L5, 120HP, 22dmg), Mire Hag (L5, 180HP, 25dmg)
- **Gorge (L6-7)**: Gorge Stalker (L6, 130HP, 26dmg), Volcanic Basilisk (L6, 150HP, 28dmg), Blackstone Marauder (L7, 140HP, 30dmg), Gorge Warden (L7, 220HP, 32dmg)

## Equipment Tiers
- L1: Leather set (armor 1-3), Iron Sword (+3 dmg), Wooden Shield (2)
- L3: Chain set (armor 3-7), Iron set boots/gauntlets/shield (2-4), Steel Longsword (+5 dmg), Amulet of Warding (3), Ring of Intellect (+2 dmg)
- L5: Enchanted Robes (4 armor, +2 dmg), Mystic Staff (+7 dmg)
- L6: Plate set (armor 4-12), Steel Greatsword (+8 dmg), Tower Shield (6)

## Known Balance Issues
- See `balance-audit-findings.md` for detailed audit
- Skill classRestriction mismatches (partially addressed)
- Melee DPS 3-5x higher than spell DPS (spell formula: stat/3 + level/2 too weak)
- Parry/Dodge rates too low at typical L1 stats (7-8% with 0.25 cap, divisor 100)
- Sneak/Pick Lock DCs never fail for designated classes
- Orphaned spells: DIAMOND_BODY (kai:3), SOOTHING_SONG/DISCORD/RALLYING_CRY (bard:2-3) — Mystic has kai:2, Bard has bard:1
- Priest has only 1 damage spell (Smite); prior audit incorrectly said "NO damage spells"
- No passive HP regen — non-healer classes dependent on potions between fights

## Proposed Balance Changes (Feb 2026 session)
- See `balance-melee-caster-gap.md` for detailed melee/caster DPS gap analysis
- Melee nerf: Add MELEE_STR_DIVISOR=3, compensate with higher weapon stats
- Spell buff: STAT_DIVISOR 3->2, LEVEL_DIVISOR 2->1, DICE 6->8, cooldowns -1
- Tier 3 spell basePower boost: Fireball 30->45, Nature's Wrath 28->42
- Tier 1 mana cost reductions (Magic Missile 5->4, etc.)
- Dodge/Parry: DIVISOR 100->80, MAX_CHANCE 0.25->0.30 (prior session)
- Mystic kai:2->3, Bard bard:1->3 (prior session)
- Priest: add Holy Smite spell, xpModifier 1.1->1.05, hpPerLevelMax 6->7 (prior session)
- New passive HP regen system (needs GameLoop code change, prior session)
- Follow-up needed: NPC HP reduction in Marsh/Gorge after melee nerf (~30%)
