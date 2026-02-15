# NeoMud TODO

Active work items and known issues, ordered roughly by priority.

## High Priority

### Game Balance Pass
- Rebalance combat damage, armor values, and HP pools across all NPC tiers
- Review XP curves — leveling may be too fast or too slow in the forest zone
- Adjust item stat budgets (weapon damage, armor values) for a smoother progression
- Tune NPC spawn rates and respawn timers
- Validate spell damage/healing scaling vs. melee at equivalent levels

### Missing Item Art
- `item:leather_chest` (Leather Vest) has no sprite — needs `assets/images/rooms/item_leather_chest.webp`
- Audit all items in `items.json` for missing icons and generate/add them

### Consumable Items from Inventory
- Tap-to-use on consumables in the bag grid currently sends the server command but UX needs validation
- Consider adding a confirmation or cooldown indicator for consumable use
- Scrolls and other single-use items should work the same way

## Medium Priority

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
