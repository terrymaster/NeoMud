# Playtester Agent Memory

## Characters Created
- **Brunak Ironbeard** (username: brunak01) -- DWARF WARRIOR, Level 1 (previous session)
- **Aelindra Starweave** (username: elfmage02, password: testpass123) -- ELF MAGE, Level 1, 66/100 XP
- **Gromm Thunderfist** (username: audiotest1, password: testpass123) -- HALF_ORC WARRIOR, Level 1, 329/100 XP (cannot level up)
- **Sister Meridia** (username: clerictest1, password: testpass123) -- HUMAN CLERIC, Level 1, 176/100 XP (cannot level up)
- **Newbie** (username: tutorial_tester, password: test1234) -- HUMAN WARRIOR, Level 1, 177/100 XP (new player experience test)

## Areas Explored
- **Town**: Temple of the Dawn (respawn, healing aura), Town Square (trainer), Market Street (blacksmith vendor), The Enchanted Emporium (magic vendor), The Rusty Tankard (barkeep vendor), North Gate (guard), Grimjaw's Forge (NPC crafter, not a vendor)
- **Forest**: Forest Edge (rats, bandits, spiders, wolves), Winding Forest Path (hidden passage north), Sunlit Clearing (safe rest spot), Deep Forest (wolves, spiders, bandits)
- **Forest/Marsh Transition**: Overgrown Ruins (wolves, leads north to marsh)
- **Marsh**: Marsh Edge (Bog Lurkers -- 14 dmg per hit, instant death for level 1)
- **Locked**: Tavern Cellar (DOWN from tavern, "The way down is locked")

## Known Bugs Already Reported
- #53 -- Multiple relay instances corrupt shared state file
- #54 -- Forest Edge spider kills level 1 players instantly with no reaction time
- #55 -- No way to discover available spells -- mage cannot figure out what to cast
- #56 -- pickup_coins does not support "all" coin type
- #57 -- No HP regeneration outside of potions (REST skill now exists, addresses this)
- #58 -- Hostile NPCs attack immediately with no grace period for new arrivals
- #104 -- scroll_of_fireball references wrong audio path (audio/items/spell_cast.mp3 but file is in audio/spells/)
- #105 -- Missing BGM files for Marsh and Gorge zones (marsh_danger.mp3, gorge_danger.mp3)
- #107 -- Kick skill description does not mention required direction parameter
- #109 -- No way to discover level up command -- trainer says ready but won't level you up
- #110 -- Rustic Dagger uses sword sounds instead of dagger-specific sounds (dagger_slash/dagger_miss exist but unused)
- #223 -- Cleric Minor Heal heals far less than Smite damages for same mana cost (6 HP heal vs 29 dmg)
- #224 -- NPC respawn is too fast -- new hostile spawns 1 second after kill
- #225 -- Rest skill event shows raw roll number -- debug info leaking to players
- #226 -- Consumable items outclass Cleric healing spells at level 1
- #233 -- No welcome message, tutorial, or gameplay hints on first login
- #234 -- Non-combat NPCs display 0/0 HP
- #235 -- No vendor announcement when entering shop rooms
- #236 -- No crafter announcement or discoverable interaction for Grimjaw's Forge
- #237 -- Purchased items not auto-equipped into empty slots
- #238 -- Loot items have no description -- players can't tell their purpose
- #239 -- Hostile NPCs attack instantly with no warning for new players
- #240 -- Tavern cellar locked with no hint about how to unlock
- #241 -- Vendor shop does not show item stats or comparison to equipped gear
- #242 -- NPC targeting requires exact instance ID with # suffix
- #243 -- Say command produces no visible feedback to the speaker
- #244 -- Forest Edge often empty -- new player waits for first enemy

## Game State Observations
- **Warrior Combat**: Iron Sword does 21-25 damage per hit (STR 35). One-shots rats (15 HP), spiders (20 HP), bandits (20 HP). Two-shots wolves (30 HP).
- **Cleric Combat**: Iron Sword does 21-24 damage per hit (STR 25). Smite does 26-31 damage (WIL 28, basePower 10). Minor Heal restores only 6 HP (basePower 10). Bash does 20 damage.
- **Warrior Skills**: Bash does ~23 damage. Kick does ~17 damage + requires direction (targetId:DIRECTION format). REST heals 4 HP/tick.
- **Cleric Skills**: Bash does 20 damage. REST heals 3 HP/tick. Meditate restores 4 MP/tick.
- **Human Warrior Combat**: Iron Sword does 25 damage per hit (STR 30). One-shots rats (15 HP), bandits (20 HP), spiders (20 HP). Two-shots wolves (30 HP). HP: 22.
- **Consumables**: Grom's Stout Ale heals 5 HP (5c) [post-rebalance], Hearty Bread Loaf heals 15 HP (12c), Health Potion heals 20 HP (20c), Mana Potion restores 20 MP (25c).
- **Economy**: Rat drops 1-7c, Spider drops 30c + Spider Fang x1, Bandit drops 9-17c, Wolf drops 6-14c + Wolf Pelt (sells 4c) + sometimes 1s. Wolf Pelt sells for 4c.
- **XP**: Rats 15 XP, Bandits 18 XP, Spiders 44 XP, Wolves 28 XP. Level 2 = 100 XP total.
- **Leveling**: BLOCKED -- cannot find level up command. Trainer says "ready to level up" but provides no action. Tried: interact_trainer, level_up command, train_level command, use_skill level_up. None work.
- **Death**: No item/coin loss, respawn at Temple with full HP (MP only restored by temple healing aura if you stay). Ground loot persists after player death.
- **Temple**: Healing aura restores HP over time ("The temple's aura soothes your wounds."). Also restores full HP/MP on respawn.
- **Safe Zones**: Temple forbids violence ("The divine sanctuary of the temple forbids violence here.")
- **Marsh**: Extremely dangerous for level 1. Bog Lurker does 14 damage per hit.
- **Audio**: Sound IDs use bare names (e.g., "sword_swing") resolved to subdirectory paths by context. Server validates at startup.

## Cleric Gameplay Notes (Session 4)
- **Spell system works well**: ready_spell + attack_toggle auto-casts each tick; cast_spell works for one-off casts out of combat
- **Smite is very strong**: 26-31 damage one-shots rats/bandits/spiders, two-shots wolves
- **Minor Heal is very weak**: Only 6 HP restored for 5 MP -- outclassed by 5c ale (10 HP) and 12c bread (15 HP)
- **Mana pool is tiny**: 10 MP total = 2 Smite casts or 1 Smite + 1 Minor Heal
- **Meditate works well**: 4 MP/tick, auto-stops when full, interrupted by combat
- **REST works**: 3 HP/tick for Cleric (vs 4 HP/tick for Warrior), interrupted by combat
- **Combat loop for Cleric**: Smite 1-2x -> melee cleanup -> meditate -> repeat. Needs safe window between fights.

## Audio Directory Structure (post-reorganization)
- `audio/bgm/` -- Background music (forest_danger, town_peaceful)
- `audio/general/` -- backstab, coin_pickup, dodge, item_pickup, loot_drop, miss, parry
- `audio/items/` -- bow_miss/shot, dagger_miss/slash, potion_drink, staff_miss/swing, sword_miss/swing
- `audio/npcs/` -- NPC attack/miss/death/interact/exit sounds
- `audio/rooms/` -- footstep_* depart sounds
- `audio/spells/` -- spell cast/impact/fizzle sounds, healing_aura

## TODOs
- Test multiplayer interactions
- Explore Blackstone Gorge
- Try leveling up once bug #109 is fixed
- Test with MAGE, DRUID, or other caster classes
- Test crafting system once Grimjaw's Forge is functional
