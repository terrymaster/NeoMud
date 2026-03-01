# WorldMaker Agent Memory

## Maker Editor Notes

### Layout
- Three-panel layout: left sidebar (nav), middle (list or map), right (properties panel)
- Top toolbar: Save As, Switch Project, Validate, Export .nmd, Package .nmd, Quit Server
- Navigation: Zones, Items, NPCs, Classes, Races, Skills, Spells, Default Players, Default SFX, Settings
- Consistent list+detail pattern across all entity types
- Default Players has sprite gallery with filter dropdowns (race/gender/class)
- Default SFX has category-filtered list with colored status dots

### Known Issues (as of Session 4 - 2026-03-01)
- Package .nmd uses native confirm() instead of proper modal (Issue #100)
- NPC list placeholder uses lowercase 'npc' (Issue #98)
- New Item form placeholder misleadingly shows 'Iron Sword' (Issue #97)
- Zone deletion has no confirmation dialog (Issue #99)
- No server-side input validation/sanitization (Issue #90)
- Room creation allows overlapping coordinates (Issue #91)
- API key visible in accessibility tree + API response despite visual masking (Issue #82)
- No search/filter on entity lists (Issue #85)
- Raw JSON error messages throughout (Issue #86)
- Class/NPC editors use raw JSON for structured data (Issue #75)
- NPC creature image dimension labels show humanoid maximums (Issue #74)

### Fixed Since Session 3
- Room ID double zone-prefix (Issue #87) -- FIXED
- Empty entity name validation (Issue #88) -- FIXED
- Grammar "A entity/item" in error messages (Issue #89) -- FIXED
- Validation results shown in browser alert() (Issue #81) -- FIXED (Validate button uses modal now)
- Grammar "a npc" in placeholder (Issue #83) -- FIXED in error messages but NOT in list placeholder

### Fixed in Earlier Sessions
- Opening existing projects (Issue #79)
- Room creation on new projects (Issue #80)
- Loot Tables nav missing (Issue #84) -- intentionally removed, merged into NPC model
- DPI canvas hit-testing (Issue #69)
- Duplicate Image Prompt fields (Issue #72)
- Item image defaults (Issue #73)
- Room creation/deletion confirmations (Issues #66, #67)
- Deleted rooms in exit dropdown (Issue #68)
- Grammar "a item" in placeholder (Issue #71)
- Debug console.log spam (Issue #65)
- Missing favicon (Issue #64)
- Cross-zone exit names show raw IDs (Issue #70)

### What Works Well
- Zone map rendering with room boxes, exit arrows, cross-zone labels
- Layer navigation (up/down buttons for multi-level zones like Tavern Cellar)
- Dynamic form fields based on item category (weapon fields appear when category set)
- Room interactable editor (lever/trapdoor with action types, stat checks, perception DC)
- Hidden exit system with perception DC, lock DC, re-hide/re-lock timers
- Exit lock system with DC, unpickable checkbox, re-lock timer
- Room rename feature (enables button when ID text changes)
- Forking read-only projects
- Default Players sprite gallery with 270 sprites and filtering
- Default SFX with color-coded category indicators
- Audio preview/player inline for BGM tracks
- Depart Sound dropdown with human-readable labels
- Export .nmd instant download
- Item creation flow: create -> auto-switch to edit mode
- Validation modal dialog (improved from alert())
- Responsive layout degrades gracefully at narrow widths
- Room effects editor (HEAL/POISON/DAMAGE/MANA_REGEN/MANA_DRAIN/SANCTUARY)
- Start Room dropdown shows "Zone > Room" format for clarity
- Target Room dropdown in exits shows "Name (zone:id)" format

### API Endpoint Map
- `/api/projects` - GET list, POST create
- `/api/projects/:name/open` - POST to open/switch
- `/api/projects/:name/fork` - POST with {newName}
- `/api/zones` - GET list, POST create
- `/api/zones/:id/rooms` - GET rooms, POST create room
- `/api/rooms/:roomId/exits` - POST create exit (NOT nested under zones)
- `/api/items` - GET list, POST create
- `/api/items/:id` - PUT update, DELETE
- `/api/npcs` - GET list, POST create
- `/api/races` - GET list
- `/api/skills` - GET list
- `/api/spells` - GET list
- `/api/default-sfx` - GET list
- `/api/settings` - GET (exposes API keys!)
- `/api/export/nmd` - GET download .nmd bundle

### Interaction Tips
- Room boxes on zone map are canvas-rendered, not DOM elements
- DPI canvas hit-testing fixed but still needs DPR ~1.0 for reliable clicking
- Right panel scrolls independently from map area
- Export endpoint is `/api/export/nmd` (not `/api/export`)
- Room creation now has a confirm dialog
- Forking _default_world is the best way to get a project with real data to test
- When creating rooms, send just the local ID (e.g., "hallway"), not "zone:hallway"
- Zone deletion cascades to rooms and NPCs correctly
- Layer navigation buttons auto-disable when at top/bottom layer
- Room properties panel persists last selected room when switching layers (not auto-cleared)
