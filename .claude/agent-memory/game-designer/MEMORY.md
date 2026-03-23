# Game Designer Agent Memory

## Key File Locations
- Data files: `maker/default_world_src/world/` (classes.json, races.json, items.json, skills.json, spells.json)
- Zone files: `maker/default_world_src/world/*.zone.json` (town, forest, marsh, gorge)
- Loot tables: Embedded in zone JSON NPC definitions (lootItems array per NPC), NOT a separate file
- GameConfig: `server/src/main/kotlin/com/neomud/server/game/GameConfig.kt`
- Combat: `server/.../game/combat/CombatManager.kt`
- Spells: `server/.../game/commands/SpellCommand.kt`
- Rest: `server/.../game/RestUtils.kt`
- Meditation: `server/.../game/MeditationUtils.kt`
- Stat allocation (shared): `shared/.../model/StatAllocator.kt`

## Core Formula Summary (CURRENT as of Mar 2026)
- HP at L1: hpPerLevelMax + (health/10)*4 + threshold bonuses
- MP at L1: mpPerLevelMax + (willpower/10)*2 (if class has MP)
- Melee damage: **STR/3** + weaponBonus + thresholdBonus + random(1..weaponRange)
- Spell power: basePower + primaryStat/**2** + level/**1** + random(1..**8**)
- Hit chance: 50 + (accuracy - defense), clamped 5-95
- Dodge: AGI/**80** * **0.30** (max 30%)
- Parry: STR/**80** * **0.30** (max 30%)
- Death penalty: 5% XP loss
- XP curve: 100 * level^2.2
- Creation CP: 60 points
- Level-up CP: 10 (L1-10), 15 (L11-20), 20 (L21-30)
- Meditation: 2 + willpower/10 MP per tick
- Rest: 1 + health/10 HP per tick

## Current Content Scope (Mar 2026)
- 6 races, 15 classes, 5 magic schools (mage/priest/druid/kai/bard)
- 4 zones: Town (7 rooms), Forest (7 rooms, L1-3), Marsh (6 rooms, L4-5), Gorge (5 rooms, L6-7)
- 12 hostile NPC types with loot tables embedded in zone JSONs
- 3 vendors, 1 trainer, 1 guard in town

## NPC Difficulty Tiers (CURRENT - post-nerf)
- **Forest (L1-3)**: Rat (L1, 15HP, 2dmg), Bandit (L1, 20HP, 3dmg), Wolf (L2, 30HP, 3dmg), Spider (L3, 20HP, 5dmg)
- **Marsh (L4-5)**: Toad (L4, 45HP, 12dmg), Lurker (L4, 55HP, 14dmg), Wraith (L5, 70HP, 17dmg), Hag (L5, 100HP, 22dmg)
- **Gorge (L6-7)**: Stalker (L6, 80HP, 26dmg), Basilisk (L6, 90HP, 28dmg), Marauder (L7, 90HP, 30dmg), Warden (L7, 140HP, 32dmg)

## Equipment Tiers (CURRENT weapon stats)
- L1: Iron Sword (+8/8), Rustic Dagger (+5/6), Wooden Staff (+3/6), Short Bow (+6/7)
- L3: Steel Longsword (+12/10), Chain armor set (3-7), Iron accessories (2-4)
- L5: Enchanted Robes (4 armor, +2 dmg), Mystic Staff (+10/10)
- L6: Plate set (4-12), Steel Greatsword (+16/12), Tower Shield (6)

## Implemented Changes (confirmed in code, Mar 2026)
- MELEE_STR_DIVISOR = 3 (melee + bash)
- Spell buffs: STAT_DIVISOR=2, LEVEL_DIVISOR=1, DICE=8
- Weapon stat updates (all in items.json)
- Magic school level enforcement in SpellCommand
- Mystic kai:3, Bard bard:3
- Priest: hpPerLevelMax 7, xpModifier 1.05, Holy Smite added
- Dodge/Parry: DIVISOR 80, MAX_CHANCE 0.30
- Rest skill added (all classes)
- NPC HP/damage reductions in Marsh (~35-45%) and Gorge (~35-40% HP)

## Outstanding Issues (Mar 2026 audit)
- See `march-2026-audit.md` for full analysis
- Forest-to-Marsh difficulty spike still 2.75x (need transitional L3-4 content)
- 5 dead-end rooms have no unique content (Cave, Stream, Clearing, Island, Alcove)
- Gypsy stuck at mage:1 -- only Magic Missile, needs mage:2
- Kai and Bard schools lack T3 damage spells
- Marsh loot drops L3 gear (already obsolete at that level)
- No L4-5 equipment tier exists
- Blacksmith doesn't sell dagger/staff/bow
- Cutting Words basePower (8) below other T1 damage spells (10-13)
- Crafting materials drop but no crafting system exists -- **design filed as issue #214**
- No loot_tables.json file exists; loot is embedded in zone NPC definitions
- **Heal spell scaling uses same formula as damage** -- heals overflow tiny L1 HP pools (#223, #227)
- **Consumables outclass heal spells at L1** -- Health Potion (25 HP) > full HP pool (#226, #228)

## Economy Analysis (Mar 2026)
- Forest farming: ~30 kills/hr, ~186c/hr from mats, ~45c/hr coins
- Marsh farming: ~20 kills/hr, ~372c/hr from mats, ~60c/hr coins
- Gorge farming: ~15 kills/hr, ~450c/hr from mats, ~75c/hr coins
- Vendor sell rate: 50-99% of item value (Charm + Haggle dependent), typical 60-75%
- Critical gap: no L4-5 equipment tier between chain (L3) and plate (L6)
- Mystic Staff corrected to +10/10 (was +8/8 in older memory)
