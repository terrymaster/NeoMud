# NeoMud TODO

Active work items and known issues, ordered roughly by priority.

## High Priority

### Game Balance (#40, #41)
- Melee DPS is 3-5x higher than spell DPS — casters are underpowered at all levels
- No passive HP regeneration outside combat (#41) — forces excessive potion purchases
- Large difficulty jump between Forest (L1-3, max 5 dmg) and Marsh (L4-5, 15+ dmg) — no L3-4 transitional content
- Priest class is unviable solo (no damage spells, worst HP, 1.2x XP penalty)
- Parry/Dodge proc rates too low at typical L1 stats (5-6%) — feels like they don't exist
- No zone danger warning when entering areas above player level (#40)

### Spell & Skill Bugs
- Magic school level number (1/2/3 in classes.json) is never enforced — all classes with any school access can cast ALL spells of that school (Ranger casts Nature's Wrath, Gypsy casts Fireball)
- Skill classRestriction mismatches: BASH missing CLERIC/WARLOCK, SNEAK missing MISSIONARY, MEDITATE missing PALADIN/MISSIONARY/BARD/GYPSY, KICK missing NINJA
- DoT/HoT tick application to NPCs needs verification — EffectApplicator uses Player object

### UX Issues (#33, #35, #42)
- Kick error message doesn't explain expected direction format (#33)
- Trainer interaction lacks context for new players (#35)
- Vendor bulk sale messages don't show quantity (#42)
- Consumable items UX — polish tap-to-use flow, add cooldown indicator

### Asset Pipeline (#36)
- `remove-bg.mjs` needs overhaul (#36) — flood fill can't reach enclosed background regions (e.g., between NPC legs), mid-tone backgrounds are skipped entirely, no matting/feathering for soft edges
- Audit items for missing sprites — most are covered after batch generation but some may remain

## Medium Priority

### Content Gaps
- Need L3-4 transitional zone or sub-zone bridging Forest and Marsh
- Dead-end rooms in Forest (cave, ruins, stream) have zero reward — add hidden items or encounters
- 5 crafting materials drop (Wolf Pelt, Spider Fang, Marsh Hide, Wraith Essence, Obsidian Shard) but no crafting system exists
- No quests, no boss encounters, no group incentives

### Skills System
- Expand active skills beyond current set
- Class-specific skill unlocks at level thresholds
- Additional passive skills and skill trees

### Hidden Content
- Hidden items in rooms (discoverable via search/perception)
- Traps (room traps, trapped locked doors, trapped objects)
- Trap disarm mechanics (rogue/thief class abilities)

### Maker Tool
- **Spells/Skills icon parity**: add `icon` field to SpellDef/SkillDef, add icon picker to maker editors, update client to prefer model field over hardcoded fallback
- **Cross-zone exit indicators**: visual map editor doesn't show when a room has an exit to another zone
- Custom world build: verify end-to-end export → server load of a non-default world
- AI generation integrations: verify 3rd party API connections
- NPC, item, and loot table editor improvements
- Validation coverage for all entity types

### Client UI
- Player status panel condensing — HP/MP/XP bars take too much vertical space in portrait mode
- Action panel reorganization — separate permanent actions (attack, hide, spell bar) from contextual ones (trainer, vendor, settings)
- Sound effects gaps: equip/unequip, spell cast, potion use, death, respawn, UI feedback sounds

### Multiplayer
- Stress testing — concurrent player interactions, combat with multiple players, reconnection handling
- Edge cases around shared combat (multiple players attacking same NPC, kill credit, loot distribution)

## Lower Priority

### Equipment & Economy
- Item rarity tiers (common → uncommon → rare → epic) with color coding
- Enchantments or item modifiers
- Money sinks beyond gear purchase — no economic drain after mid-game

### Quest System
- Kill quests, fetch quests, escort quests
- Quest log UI panel
- Quest rewards (XP, items, coins)
- Quest-gating for zone access

### NPC Dialogue
- Conversation trees for friendly NPCs
- Quest givers with dialogue-driven quest acceptance
- Lore NPCs that expand world-building

### Future Platforms
- Web client
- iOS client
