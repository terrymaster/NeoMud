---
name: rebuild-world
description: >
  Rebuild the default_world.nmd bundle after changes to maker/default_world_src/.
  The server loads this .nmd file (a ZIP archive) on startup, similar to DOOM WAD files.
  Raw assets live in the codebase but must be packaged into the .nmd to take effect.
autoTrigger: true
---

# Rebuild Default World Bundle

Rebuild `default_world.nmd` after any change to files under `maker/default_world_src/`.

## When to Run

Run this **every time** files are modified in `maker/default_world_src/`, including:
- Zone JSON files (`world/*.zone.json`)
- Catalog files (`world/items.json`, `world/spells.json`, `world/skills.json`, etc.)
- Asset files (`assets/images/`, `assets/audio/`)
- The manifest (`manifest.json`)

## Steps

1. **Export JAVA_HOME** (required — not set system-wide):
   ```
   export JAVA_HOME=/c/Users/lbarnes/.jdks/corretto-21.0.5
   ```

2. **Run packageWorld** with `--rerun-tasks` to force rebuild (Gradle UP-TO-DATE checks sometimes miss JSON changes):
   ```
   ./gradlew packageWorld --rerun-tasks
   ```

3. **Verify** the output exists:
   - `server/build/worlds/default-world.nmd` (used by server at runtime)
   - `maker/default_world.nmd` (copy for maker import)

## What packageWorld Does

- Zips `maker/default_world_src/` contents (manifest.json, world/**, assets/**) into a `.nmd` file
- Places the output in `server/build/worlds/default-world.nmd`
- Copies to `maker/default_world.nmd`

## If the Maker Dev Server Is Running

If you also need the maker's Prisma DB to reflect changes (e.g., for testing in the maker UI):
1. Kill the Vite dev server first (`netstat -ano | grep :5173`, then `taskkill //PID <pid> //F`)
2. Run `cd maker && npm run rebuild-world`
3. Restart Vite: `cd maker && npm run dev`

The `rebuild-world` npm script regenerates the Prisma client, re-zips, deletes the stale DB, and re-imports.
