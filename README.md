# NeoMud

A love letter to the MUDs of the '90s, built with modern tools and vibes. 100% vibe-coded with AI.

<p align="center">
  <img src="docs/screenshots/screenshot1.png" width="230" alt="Winding Forest Path" />
  <img src="docs/screenshots/screenshot2.png" width="230" alt="Forest Edge" />
  <img src="docs/screenshots/screenshot3.png" width="230" alt="Character Sheet" />
</p>

## What Is This?

NeoMud is a multiplayer dungeon game inspired by the text-based MUDs (Multi-User Dungeons) that consumed countless hours on dial-up connections in the 1990s. Games like MajorMUD, Legends of Kesmai, and the countless DikuMUD derivatives that ran on BBSes and early internet servers — where imagination filled in what pixels couldn't.

This project is a tribute to that era, but it doesn't try to be a museum piece. It takes the core of what made MUDs great — exploration, combat, character progression, and shared worlds — and wraps it in a modern mobile client with room art, sprite overlays, and a real-time WebSocket backbone. The text log is still there. The direction pad is still there. But now you can *see* the tavern you're drinking in.

## Why Vibe Code a MUD?

Because MUDs were the original MMOs, and they got a lot of things right that modern games lost along the way:

- **Worlds driven by data, not code.** Rooms, items, NPCs, loot tables, spells, and skills are all JSON. A game master can reshape the world without recompiling anything.
- **Emergent multiplayer.** You share a room with other players. You see them arrive and leave. You talk. You fight the same monsters. No instancing, no sharding — just a shared world.
- **Mechanical transparency.** You know your stats. You know your weapon damage. You can reason about the systems, and that reasoning is the game.

This project is vibe-coded — built iteratively with AI assistance, following intuition over architecture docs, letting the design emerge from play. It's not production software. It's a playground.

## Architecture

```
NeoMud/
├── shared/     Kotlin Multiplatform — models and protocol shared between client and server
├── server/     Ktor + Netty — WebSocket game server with SQLite persistence
├── client/     Jetpack Compose — Android client with sprite rendering
└── maker/      React + Express — web-based world editor and GM toolkit
```

**Server** runs a 1.5-second tick-based game loop. NPCs wander, patrol, and attack. Combat resolves each tick. Loot drops. Spells cool down. The world turns.

**Client** connects over WebSocket and renders the game as a layered scene: room background, NPC and item sprites, floating minimap, game log, and controls.

**Shared** module contains the protocol (sealed classes with `kotlinx.serialization`) and all data models. Client and server speak the same language at compile time.

**Maker** is a web-based world editor (React frontend, Express API, SQLite per-project databases) for building zones, rooms, NPCs, items, and more — with visual map editing and export to the server's `.nmd` bundle format.

## Features

### The World
- Two zones: the town of Millhaven (safe) and the Whispering Forest (dangerous)
- 17 rooms with hand-crafted background art (AI-generated, background-removed, served as WebP)
- JSON-driven room definitions with coordinates, exits, and background images
- BFS-based minimap showing nearby rooms with player/NPC presence indicators
- Temple of the Dawn healing aura — passively restores HP each tick while you rest
- `.nmd` world bundles — self-contained ZIP archives with all zone data, catalogs, and assets
- Asset validation at world load time — missing images or audio flagged on startup

### Characters
- 6 races: Human, Dwarf, Elf, Halfling, Gnome, Half-Orc — each with stat modifiers and XP scaling
- 15 character classes: Warrior, Paladin, Witch Hunter, Cleric, Priest, Missionary, Mage, Warlock, Druid, Ranger, Thief, Ninja, Mystic, Bard, Gypsy
- 6-stat system: Strength, Agility, Intellect, Willpower, Health, Charm
- 60 CP stat allocation at character creation with escalating costs above class minimums
- Equipment slots: weapon, shield, head, chest, hands, legs, feet — managed via paperdoll screen

### Progression
- 30 level cap with XP from combat (scaled by level difference)
- CP gains per level: 10 (levels 1–10), 15 (levels 11–20), 20 (levels 21+)
- Trainers for spending CP on stat increases
- 10% XP penalty on death

### Spells & Skills
- 5 magic schools: Mage, Priest, Druid, Kai, Bard
- 20 spells with mana costs, cooldowns, and school-based progression
- 12 skills: combat (Bash, Kick, Backstab, Parry, Dodge), stealth (Hide, Sneak), utility (Meditate, Perception, Pick Lock, Track, Haggle)
- Spell bar UI with drag-to-assign slots

### Combat
- Tick-based (1.5s) — no button mashing, just tactical decisions
- Weapon damage = Strength + bonus + random roll; armor reduces incoming damage
- NPCs attack on sight if hostile; perception checks reveal hidden players
- Per-NPC HP tracking — multiple spawned copies of the same NPC update independently via unique IDs
- Backstab from stealth for bonus damage
- Death respawns you at the Temple of the Dawn with a "YOU DIED" overlay

### NPCs & Spawning
- 5 behavior types: idle, patrol, wander, trainer, vendor
- Continuous spawn system — hostile NPCs respawn over time to maintain zone population
- Per-zone spawn config: max entities, max per room, spawn rate in ticks
- Spawned copies share template sprites — `shadow_wolf#3` resolves to `shadow_wolf.webp` automatically
- NPC perception vs player stealth rolls each tick

### Items & Economy
- Data-driven items: weapons, armor, consumables, loot drops
- Loot tables per NPC with weighted drops
- Four-tier coin system: Copper, Silver, Gold, Platinum
- Ground loot rendered as clickable sprites

### Multiplayer
- Real-time WebSocket sessions
- Room-based chat
- See other players enter and leave
- One session per account
- Player state persisted on disconnect — log back in where you left off with full room state restored
- Game state mutex and rate limiter for server-side hardening

### Audio
- Per-zone background music with crossfade on zone transitions
- Sound effects for combat, movement, spells, and item interactions
- NPC interact sounds — audio feedback when opening trainer or vendor panels
- Configurable volume controls for BGM and SFX independently

### Active Skills
- Class-based skill activation (Bash, Kick, Backstab, Hide, etc.) via action panel
- Locked exits requiring keys or items to pass
- Contextual action buttons — permanent class actions separated from situational ones (shop, train, level up)

### Client Navigation
- 10-direction pad: N, S, E, W, NE, NW, SE, SW, Up, Down
- Minimap with clickable room navigation

## NeoMUD Maker

A web-based world editor for building and managing game worlds without touching JSON files.

### What's Working
- **Project management** — create, open, delete, and switch between world projects (each backed by its own SQLite database)
- **Default world import** — auto-imports the server's `default-world.nmd` bundle as a read-only reference project on first startup
- **Read-only projects** — the default world can be browsed but not modified; fork it to create an editable copy
- **Visual zone editor** — drag-and-drop room placement on a grid canvas, click-to-connect exits with automatic bidirectional linking
- **Zone/room/exit CRUD** — full create, update, delete for zones, rooms, and exits via REST API
- **Entity editors** — full CRUD for all catalog types: Items, NPCs, Classes, Races, Skills, Spells, Loot Tables
- **Image previews** — room backgrounds and NPC sprites shown inline in editors
- **Prompt template editor** — configure AI art generation prompts per entity
- **Export** — export any project as `.nmd` bundle or raw JSON for the game server
- **Import** — import existing `.nmd` bundles into new maker projects
- **Prisma schema** — 12 entity types: Zone, Room, Exit, Item, NPC, CharacterClass, Race, Skill, Spell, LootTable, PromptTemplate, ProjectMeta

### Running the Maker
```bash
cd maker
npm install
npm run dev
```
Opens on `http://localhost:5173` (Vite frontend) with the API server on port 3001.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.1 (JVM 21) |
| Server | Ktor 3.0 + Netty |
| Database | SQLite + Exposed ORM |
| Client | Jetpack Compose + Material 3 |
| Images | Coil 2.7 (WebP with transparency) |
| Audio | Android MediaPlayer + SoundPool |
| Protocol | kotlinx.serialization over WebSocket |
| Shared Code | Kotlin Multiplatform |
| Build | Gradle 8.11 with configuration cache |
| Maker | React 18 + Express + Prisma (SQLite) |

## Running It

### Prerequisites
- JDK 21 (e.g., Amazon Corretto)
- Android SDK with platform 34
- Android emulator or device (min SDK 26)
- Node.js 18+ (for the Maker)

### Server
```bash
export JAVA_HOME=/path/to/jdk21
./gradlew :server:run
```
The server starts on port 8080 with WebSocket at `/game` and health check at `/health`.

### Client
```bash
./gradlew :client:installDebug
```
Connect to `10.0.2.2:8080` from the emulator (or your server's IP from a device).

### Maker
```bash
cd maker
npm install
npm run dev
```
Opens the world editor at `http://localhost:5173`. On first run, it auto-imports the default world if `server/build/worlds/default-world.nmd` exists.

## Roadmap

This is an active project. Here's what exists, what's in progress, and where it's headed.

### What's Built
- [x] WebSocket multiplayer with real-time room presence
- [x] Two zones (town + forest) with 17 rooms
- [x] 6 races with stat modifiers and XP scaling
- [x] 15 character classes with stat-based differentiation
- [x] 60 CP stat allocation at registration with escalating costs
- [x] Tick-based combat with hostile NPCs
- [x] Stealth system with hide, backstab, and NPC perception checks
- [x] 5 magic schools, 20 spells with cooldowns and mana costs
- [x] 12 skills (combat, stealth, utility)
- [x] XP and leveling system (30 level cap) with trainers
- [x] Inventory icon grid with item sprites and tap-to-use consumables
- [x] Paperdoll equipment screen with slot-based equip/unequip
- [x] Vendor system with buy/sell tabs and charm-based pricing
- [x] Loot tables and ground item drops
- [x] Four-tier coin economy
- [x] Continuous NPC spawn system per zone
- [x] Room background art and sprite overlays
- [x] Minimap, game log, character sheet, spell bar
- [x] Death/respawn with XP penalty and temple spawn point
- [x] Room healing aura (Temple of the Dawn)
- [x] Player state persistence across sessions
- [x] 10-direction navigation (cardinal, diagonal, vertical)
- [x] Audio system with BGM, SFX, NPC interact sounds, and volume controls
- [x] `.nmd` world bundle format with asset validation — server loads exclusively from bundles
- [x] Active skills with class-based activation and locked exits
- [x] Action panel with permanent class actions separated from contextual buttons
- [x] Admin system with slash commands and inline autocomplete
- [x] Multiplayer hardening — game state mutex, rate limiter, frame size cap
- [x] NeoMUD Maker — web-based world editor with all entity editors, visual map canvas, import/export
- [x] Default world import with read-only projects and fork workflow

### Up Next
- [ ] **Game Balance Pass** — rebalance combat, XP curves, item stats, and NPC difficulty across all content
- [ ] **Missing Item Art** — add icons for items without sprites (e.g., leather chest piece)
- [ ] **Player Status Condensing** — compact the HP/MP/XP status panel, explore horizontal orientation
- [ ] **Equipment Upgrades** — tiered gear, enchantments, item rarity system
- [ ] **NPC Dialogue** — conversation trees, quest givers, lore NPCs
- [ ] **Quest System** — kill quests, fetch quests, quest log, rewards

### NeoMUD Maker — Next Steps
- [ ] **AI Art Pipeline** — generate room backgrounds, NPC sprites, and item icons from prompt templates
- [ ] **AI Sound Design** — generate ambient audio, combat sounds, and zone music from descriptions
- [ ] **Live Preview** — test your world in-client without restarting the server
- [ ] **Publish & Share** — export complete `.nmd` bundles for other NeoMud servers

### Future Vision
- [ ] **More Zones** — dungeons, caves, swamps, castles — each with unique NPCs and loot
- [ ] **Crafting** — gather materials, combine into items, crafting recipes
- [ ] **Party System** — group up, shared XP, party chat, group combat tactics
- [ ] **PvP** — optional dueling, arenas, or PvP zones
- [ ] **Guilds/Clans** — player organizations, shared banks, guild halls
- [ ] **Boss Encounters** — multi-phase fights, special mechanics, rare drops
- [ ] **Status Effects Expansion** — poison, stun, bleed, buffs/debuffs with tactical depth
- [ ] **Emotes & Social** — /wave, /bow, roleplay support
- [ ] **World Events** — timed spawns, invasions, seasonal content
- [ ] **iOS Client** — Compose Multiplatform or SwiftUI
- [ ] **Web Client** — browser-based alternative using the same WebSocket protocol

### Someday/Maybe
- [ ] Procedural zone generation
- [ ] Player housing
- [ ] Economy simulation (supply/demand pricing)
- [ ] Achievement system
- [ ] Leaderboards
- [ ] LLM-powered NPC conversation

## The Spirit of the Thing

This isn't a finished game. It's a living sketch — a place to experiment with what a MUD looks like when you can see it, when the protocol is type-safe, when the world data lives in version control alongside the code.

If you played MUDs in the '90s, you'll recognize the bones. If you didn't, maybe this will show you what all the fuss was about.
