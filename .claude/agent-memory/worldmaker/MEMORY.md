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

### Known Issues (Session 3 - 2026-03-01)
- Room ID gets double zone-prefix when client sends fully-qualified ID (Issue #87)
- No validation for empty entity names (Issue #88)
- Grammar "A entity/item" systemic across all entity types (Issue #89)
- No server-side input validation/sanitization (Issue #90)
- Room creation allows overlapping coordinates (Issue #91)
- Validation results shown in browser alert() not proper UI (Issue #81)
- API key visible in accessibility tree + API response despite visual masking (Issue #82)
- Grammar: "a npc" -> "an NPC" (Issue #83)
- No search/filter on entity lists (Issue #85)
- Raw JSON error messages throughout (Issue #86)
- Class/NPC editors use raw JSON for structured data (Issue #75)

### Fixed Since Last Session
- Opening existing projects (Issue #79) -- FIXED
- Room creation on new projects (Issue #80) -- FIXED
- Loot Tables nav missing (Issue #84) -- FIXED
- DPI canvas hit-testing (Issue #69) -- FIXED
- Duplicate Image Prompt fields (Issue #72) -- FIXED
- Item image defaults (Issue #73) -- FIXED
- NPC image dimension labels (Issue #74) -- FIXED
- Room creation/deletion confirmations (Issues #66, #67) -- FIXED
- Deleted rooms in exit dropdown (Issue #68) -- FIXED
- Grammar "a item" in placeholder (Issue #71) -- FIXED
- Debug console.log spam (Issue #65) -- FIXED
- Missing favicon (Issue #64) -- FIXED

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
- NO API routes: /api/classes, /api/default-players, /api/validate, /api/loot-tables

### Interaction Tips
- Room boxes on zone map are canvas-rendered, not DOM elements
- DPI canvas hit-testing fixed (Issue #69)
- Right panel scrolls independently from map area
- Export endpoint is `/api/export/nmd` (not `/api/export`)
- Room creation now has a confirm dialog
- Forking _default_world is the best way to get a project with real data to test
- When creating rooms, send just the local ID (e.g., "hallway"), not "zone:hallway" -- server adds prefix
- Zone deletion cascades to rooms and NPCs correctly
