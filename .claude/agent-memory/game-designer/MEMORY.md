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

## Core Formula Summary (CURRENT as of Apr 2026)
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

## Armor Formula Detail (CRITICAL)
- NPC damage to player: `(npc.damage + random(1..npc.damage/3)) - totalArmorValue - parryReduction`, min 1
- This is FLAT SUBTRACTION -- armor fully negates damage below its value
- Chain (24 total) negates ALL marsh NPC base damage (12-22)
- Fix needed: divide totalArmorValue by 2 before subtraction

## Current Content Scope (Apr 2026)
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

## Outstanding Issues (Apr 2026 audit)
- **CRITICAL**: Flat armor subtraction trivializes Marsh -- chain (24) negates all L4-5 NPC damage to min 1
  - Proposed fix: ARMOR_REDUCTION_DIVISOR = 2 in GameConfig, halves effective armor
- No L4-5 equipment tier exists (proposed: Scale armor set, L4 weapons)
- Marsh loot drops L3 gear (already obsolete at that level) -- needs L4-5 drops
- 4 dead-end rooms have no content (Cave, Stream, Island, Alcove; Clearing is sanctuary = fine)
- Blacksmith doesn't sell intermediate dagger/bow/staff upgrades (crafted ones exist but not vendored)
- No loot_tables.json file exists; loot is embedded in zone NPC definitions
- Crafting system implemented (issue #214 complete)
- Gypsy has mage:2, can cast MM + Frost Bolt + Arcane Shield -- actually fine
- Kai and Bard T3 damage spells now exist (Dragon Fist, Thunderous Crescendo)
- Cutting Words basePower now 11 -- close to parity
- Heal scaling uses HEAL_STAT_DIVISOR=4 (half of damage) -- adequate
- Health Potion heals 15 (not 25 as previously noted)

## Economy Analysis (Mar 2026)
- Forest farming: ~30 kills/hr, ~186c/hr from mats, ~45c/hr coins
- Marsh farming: ~20 kills/hr, ~372c/hr from mats, ~60c/hr coins
- Gorge farming: ~15 kills/hr, ~450c/hr from mats, ~75c/hr coins
- Vendor sell rate: 50-99% of item value (Charm + Haggle dependent), typical 60-75%
- Critical gap: no L4-5 equipment tier between chain (L3) and plate (L6)
- Mystic Staff corrected to +10/10 (was +8/8 in older memory)
- Crafted weapons: Venom Dagger +7/7 L3, Wolfbone Bow +8/8 L3, Marsh Reed Staff +9/9 L3, Obsidian Edge +14/11 L6, Wraith Blade +15/10 L7
- Crafted armor fills L2-3 (marsh hide vest 5, legs 4, boots 3) and L5 (wraith hood 4, obsidian buckler 5)
- Full equipment audit in items.json: 9 weapons, 24 armor, 9 consumables, 4 crafting mats
