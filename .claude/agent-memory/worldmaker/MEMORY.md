# WorldMaker Agent Memory

## Maker Editor Notes

### Layout
- Three-panel layout: left sidebar (nav), middle (list or map), right (properties panel)
- Top toolbar: Save As, Switch Project, Validate, Export .nmd, Package .nmd, Quit Server
- Navigation: Zones, Items, NPCs, Classes, Races, Skills, Spells, Loot Tables, Default Players, Default SFX, Settings
- Consistent list+detail pattern across all entity types

### Known Issues (Session 2026-02-28)
- **Critical**: Canvas map click hit-testing broken at DPI >1.0 (Issue #69) -- use 1280x720 viewport
- Room creation on single click, no confirm (Issue #66)
- Room deletion has no confirmation (Issue #67), but Item/NPC deletion does
- Deleted rooms persist in exit dropdown (Issue #68)
- Debug console.log spam from LayerDebug (Issue #65)
- Missing favicon 404 (Issue #64)
- Grammar: "a item" -> "an item" (Issue #71)
- Duplicate Image Prompt fields in all entity editors (Issue #72)
- New item image defaults to 1024x576 instead of 256x256 (Issue #73)
- NPC image dimension max labels swapped for creatures (Issue #74)
- Raw JSON fields for structured data (Issue #75)
- Cross-zone exit names show raw IDs (Issue #70)

### Interaction Tips
- Room boxes on zone map are canvas-rendered, not DOM elements
- At DPI 1.5, click coordinates misalign -- use 1280x720 viewport as workaround
- Right panel scrolls independently from map area
- Export and Package both download .nmd; Package validates first
