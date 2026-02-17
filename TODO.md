# NeoMud TODO

Active work items and known issues, ordered roughly by priority.

## High Priority

### Room Effects — Future Work
- ~~HEAL, POISON, DAMAGE, MANA_REGEN, MANA_DRAIN, SANCTUARY implemented~~ ✓
- Effects should respect immunities or resistances if added later
- Consider room-based stat buff/debuff effects

### Game Balance Pass
- Rebalance combat damage, armor values, and HP pools across all NPC tiers
- Review XP curves — leveling may be too fast or too slow in the forest zone
- Adjust item stat budgets (weapon damage, armor values) for a smoother progression
- Tune NPC spawn rates and respawn timers
- Validate spell damage/healing scaling vs. melee at equivalent levels

### Missing Assets in Default World
- `item:leather_chest` (Leather Vest) has no sprite
- Audit all items in `items.json` for missing icons and generate/add them
- Audit sound effects — identify missing SFX referenced in zone data
- Generate/source missing assets

### Consumable Items UX
- Healing potion works from inventory but UI is janky — polish the tap-to-use flow
- Add confirmation or cooldown indicator for consumable use
- Scrolls and other single-use items should work the same way

### Sound Effects System
- ~~Combat hits~~ ✓
- ~~Item pickup~~ ✓
- Equip/unequip sounds
- Spell casting, potion use sounds
- Death sound and respawn chime
- UI feedback sounds: button taps, panel open/close, error buzzes
- Volume controls in settings panel

### Dependency Upgrades
- **Coil 2 → 3**: Package rename `coil` → `coil3`, maven coords change, ~6 files. 25-40% Compose perf gain.
- **AGP 8.9 → 9.0**: May need Gradle wrapper update. Built-in Kotlin support in AGP 9.
- **Exposed 0.57 → 1.0**: Full package rename + API changes. Do separately, high effort.
- **React 18 → 19**: Coordinated bump of react, react-dom, @types/react, @types/react-dom. Breaking changes in hooks and legacy APIs.
- **Prisma 6 → 7**: Must bump `prisma` and `@prisma/client` together + run `prisma generate`. Engine moved to WASM.

## Medium Priority

### Skills System
- Expand active skills beyond current implementation
- Skill cooldowns, resource costs, and scaling
- Passive skills and skill trees
- Class-specific skill unlocks at level thresholds

### Hidden Content
- Hidden items in rooms (discoverable via search/perception)
- Hidden pathways / secret exits
- Room-based interactive objects (levers, chests, altars, etc.)
- Traps (room traps, trapped locked doors, trapped objects)
- Detection mechanics (class abilities, item bonuses, skill checks)
- Trap disarm mechanics (rogue/thief class abilities, items)

### Maker Tool
- **Custom world build**: verify end-to-end export → server load of a non-default world
- **AI generation integrations**: verify 3rd party API connections (image gen, text gen)
- **Generation UI**: incorporate buttons/workflows for AI-generated room art, descriptions, NPC dialogue
- NPC editor improvements
- Item editor improvements
- Loot table editor improvements
- Asset management UI (upload, preview, organize)
- Validation coverage for all entity types

### Player Status Panel Condensing
- The HP/MP/XP bars take up significant vertical space in portrait mode
- Explore a horizontal/compact layout for the status section
- Consider collapsing XP bar into a smaller indicator (level badge + progress arc?)
- Status panel should leave more room for the game log and controls

### Action Panel Reorganization
- **Permanent class actions**: attack toggle, hide/stealth, spell bar — always visible
- **Contextual actions**: trainer (star), vendor (shop), settings (gear) — only when relevant NPCs are present
- Currently all buttons compete for the same row; separate into tiers
- Contextual buttons could appear as floating prompts near the room view or in a secondary row
- Equipment and bag buttons should remain permanently accessible

## Lower Priority

### Equipment Upgrades
- Tiered gear (common → uncommon → rare → epic)
- Enchantments or item modifiers
- Item rarity color coding in inventory and equipment panels

### NPC Dialogue
- Conversation trees for friendly NPCs
- Quest givers with dialogue-driven quest acceptance
- Lore NPCs that expand world-building

### Quest System
- Kill quests, fetch quests, escort quests
- Quest log UI panel
- Quest rewards (XP, items, coins)
- Quest-gating for zone access or item availability
