# WorldMaker Agent Memory

## Maker Editor Notes

### Layout
- Three-panel layout: left sidebar (nav), middle (list or map), right (properties panel)
- Top toolbar: Save As, Switch Project, Validate, Export .nmd, Package .nmd, Quit Server
- Navigation: Zones, Items, NPCs, Classes, Races, Skills, Spells, Default Players, Default SFX, Settings
- Loot Tables navigation was removed/missing as of 2026-03-01 (Issue #84)
- Consistent list+detail pattern across all entity types
- Default Players has sprite gallery with filter dropdowns (race/gender/class)
- Default SFX has category-filtered list with colored status dots

### Known Issues (Session 2026-03-01)
- **Critical**: Opening existing projects fails with Prisma --accept-data-loss error (Issue #79)
- **Critical**: Room creation fails with 400 on new projects (Issue #80)
- Validation results shown in browser alert() not proper UI (Issue #81)
- API key visible in accessibility tree despite visual masking (Issue #82)
- Grammar: "a npc" -> "an NPC" (Issue #83)
- Loot Tables navigation missing from sidebar (Issue #84)
- No search/filter on entity lists (Issue #85)
- Raw JSON error messages throughout (Issue #86)

### Previous Session Issues (2026-02-28)
- Canvas map click hit-testing broken at DPI >1.0 (Issue #69) -- use 1280x720 viewport
- Room creation on single click, no confirm (Issue #66) -- NOW FIXED, confirm dialog added
- Room deletion has no confirmation (Issue #67), but Item/NPC deletion does
- Deleted rooms persist in exit dropdown (Issue #68)
- Debug console.log spam from LayerDebug (Issue #65)
- Missing favicon 404 (Issue #64)
- Grammar: "a item" -> "an item" (Issue #71)
- Duplicate Image Prompt fields in all entity editors (Issue #72)
- New item image defaults to 1024x576 instead of 256x256 (Issue #73)
- NPC image dimension max labels swapped for creatures (Issue #74) -- still present
- Raw JSON fields for structured data (Issue #75)
- Cross-zone exit names show raw IDs (Issue #70) -- NOW FIXED

### What Works Well
- Zone map rendering with room boxes, exit arrows, cross-zone labels
- Dynamic form fields based on item category (weapon fields appear when category set)
- Forking read-only projects
- Default Players sprite gallery with 270 sprites and filtering
- Default SFX with color-coded category indicators
- Audio preview/player inline for BGM tracks
- Depart Sound dropdown with human-readable labels
- Export .nmd instant download
- Item creation flow: create -> auto-switch to edit mode

### Interaction Tips
- Room boxes on zone map are canvas-rendered, not DOM elements
- At DPI 1.5, click coordinates misalign -- use 1280x720 viewport as workaround
- Right panel scrolls independently from map area
- Export and Package both download .nmd; Package validates first
- Room creation now has a confirm dialog (fixed since last session)
- Forking _default_world is the best way to get a project with real data to test
- New empty projects have broken room creation (Issue #80)
