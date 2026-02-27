---
name: game-designer
description: Launch a game design session to analyze balance, propose content, and audit RPG mechanics across NeoMud's world data
context: fork
agent: game-designer
---

# Game Design Session

Launch a game design session to analyze NeoMud's RPG systems, identify balance issues, and propose concrete improvements. The game-designer agent reads all world data files, models the math, and produces actionable design proposals.

## Focus Area

$ARGUMENTS

If a focus area was provided above, concentrate your analysis on that specific topic (e.g., "forest zone difficulty", "loot table balance", "new dungeon zone", "class viability at level 5", "economy and gold sinks"). Read the relevant data files and provide deep, targeted analysis with specific proposals.

If no focus area was provided, run a full world audit: scan all zones, items, NPCs, loot tables, and class data for gaps, imbalances, and opportunities.

## Prerequisites

Read all world data files to build a complete picture of current game state:

```
maker/default_world_src/world/classes.json    — 15 character classes
maker/default_world_src/world/races.json      — 6 races
maker/default_world_src/world/items.json      — all weapons, armor, consumables
maker/default_world_src/world/skills.json     — active and passive skills
maker/default_world_src/world/spells.json     — spells across 5 schools
maker/default_world_src/world/loot_tables.json — drop tables per NPC type
maker/default_world_src/world/*.zone.json     — all zone definitions (rooms, NPCs, exits)
```

Also read the combat formula constants:

```
server/src/main/kotlin/com/neomud/server/game/GameConfig.kt
```

## Session Flow

1. **Read all data files** listed above. Build a mental model of the current world: zones, NPC difficulty curves, equipment tiers, loot progression, class capabilities, and economy.

2. **Analyze against fun criteria.** Apply the five tests from your persona (Discovery, Choice, Progression, Balance, Surprise) to the current state. Model the math — expected damage, hit rates, time-to-kill, reward pacing.

3. **Identify gaps and imbalances.** Look for: difficulty spikes between zones, dead-end content with no reward, classes that are unviable, loot tables that are too generous or too stingy, equipment tiers with no content to bridge them.

4. **Propose specific changes.** Every proposal must be implementable — include the exact JSON changes, new stat blocks, or formula adjustments. For new visual content (NPCs, items), include `imagePrompt`, `imageStyle`, and `imageNegativePrompt` fields.

5. **Present a structured design document** for review.

## Output Format

Structure your output as:

### Current State Summary
Key numbers and world snapshot relevant to the focus area.

### Analysis
What's working, what's not, and why — backed by math (expected values, probability distributions, time-to-kill estimates).

### Proposals (Prioritized)
Numbered list from highest to lowest impact. Each proposal includes:
- **What**: The specific change
- **Why**: What problem it solves
- **How**: Exact JSON data or formula changes needed
- **Impact**: What this affects and expected outcome

### Implementation Notes
- Which files need editing
- Any new assets that need generation (with image prompts)
- Any server code changes required (flag these — they need developer review)
- Dependencies between proposals

## Constraints

- Prefer data-file changes (JSON) over server code changes. If a code change is needed, flag it explicitly.
- Respect existing JSON schemas — match the field names and structure of existing zone/item/NPC definitions.
- Include image prompt suggestions (`imagePrompt`, `imageStyle`, `imageNegativePrompt`, `imageWidth`, `imageHeight`) for any new NPCs, items, or rooms.
- Remember the asset pipeline: new sprites need generation via nano-banana, conversion to WebP, and background removal via `scripts/remove-bg.mjs`.
- Check your agent memory first — don't re-derive formulas or data you've already recorded.
