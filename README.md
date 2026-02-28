# NeoMud

A love letter to the MUDs of the '90s, built with modern tools and vibes. 100% vibe-coded with AI.

<p align="center">
  <img src="docs/screenshots/screenshot1.png" width="230" alt="Town Square with stone-framed room view and game log" />
  <img src="docs/screenshots/screenshot2.png" width="230" alt="Forest combat with Shadow Wolf — loot drops and XP gain" />
</p>
<p align="center">
  <img src="docs/screenshots/screenshot3.png" width="230" alt="Character sheet — stats, equipment, skills, and coins" />
  <img src="docs/screenshots/screenshot4.png" width="230" alt="Blacksmith vendor panel with buy and sell tabs" />
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
├── maker/      React + Express — web-based world editor and GM toolkit
└── .claude/    Agents, skills, and memory for Claude Code AI tooling
```

**Server** runs a 1.5-second tick-based game loop. All combat actions — melee, spells, bash, kick — resolve in initiative order each tick. Non-combat skills (meditate, track) resolve in a pre-combat phase. All NPC kills flow through a single handler for loot, XP, and state cleanup. NPCs wander, patrol, pursue, and attack. The world turns.

**Client** connects over WebSocket and renders the game as a layered scene: room background, NPC and item sprites, floating minimap, game log, and controls.

**Shared** module contains the protocol (sealed classes with `kotlinx.serialization`) and all data models. Client and server speak the same language at compile time.

**Maker** is a web-based world editor (React frontend, Express API) for building zones, rooms, NPCs, items, and more — with visual map editing and export to the server's `.nmd` bundle format.

## Current State

This is an early-stage hobby project. The core systems work and there's enough content for a real play session, but balance is rough and many features need playtesting. Here's an honest breakdown.

### What Works

**The World**
- 4 zones with 25 rooms: Millhaven (safe town, 7 rooms), Whispering Forest (L1-3, 7 rooms), Thornveil Marsh (L4-5, 6 rooms), Blackstone Gorge (L6-7, 5 rooms)
- AI-generated background art for every room (WebP)
- JSON-driven room definitions with coordinates, exits, and background images
- Room effects — healing aura (Temple of the Dawn), poison, mana drain, sanctuary
- Hidden exits and interactable objects (levers, chests, altars) with skill-check gating
- BFS-based minimap with fog-of-war, zone color-coding, and exit indicators (locked, hidden, interactable)
- `.nmd` world bundles — self-contained ZIP archives with zone data, catalogs, and assets

**Characters**
- 6 races: Human, Dwarf, Elf, Halfling, Gnome, Half-Orc — each with stat modifiers and XP scaling
- 15 character classes: Warrior, Paladin, Witch Hunter, Cleric, Priest, Missionary, Mage, Warlock, Druid, Ranger, Thief, Ninja, Mystic, Bard, Gypsy
- 6-stat system: Strength, Agility, Intellect, Willpower, Health, Charm
- 60 CP stat allocation at creation with escalating costs above class minimums
- 9 equipment slots: weapon, shield, head, chest, hands, legs, feet, neck, ring
- Starter equipment granted on character creation (class-appropriate weapon + armor)
- 270 unique player sprites (race/gender/class combinations)

**Combat & NPCs**
- Tick-based (1.5s) combat — weapon damage = Strength + bonus + random roll; armor reduces incoming
- Unified tick-based skill resolution — bash, kick, spells queue on the session and resolve each tick in initiative order
- Spell auto-cast — ready a spell once, it repeats each tick until cancelled, interrupted, or out of MP
- 12 hostile NPC types across 3 combat zones scaling from L1 (15 HP, 2 dmg) to L7 (220 HP, 32 dmg)
- 5 town NPCs: guards, vendors, trainers
- Parry (STR-scaled, class-gated) and Dodge (AGI-scaled, class-gated)
- Backstab from stealth — server-authoritative, bonus damage on first hit
- Kick knockback into adjacent rooms with direction picker
- Bash stun for multiple ticks
- Hostile NPC pursuit — engaged NPCs chase fleeing players
- Combat grace period on room entry
- Death respawns at temple with 10% XP penalty
- Continuous NPC spawn system per zone with configurable rates

**Items & Economy**
- 41 data-driven items: 7 weapons (L1-6), leather/chain/plate armor sets, consumables, scrolls, crafting materials
- 12 loot tables across all hostile NPC types
- 3 vendors in town: tavern (potions, food), blacksmith (weapons, armor), enchantress (scrolls, enchanted gear)
- Four-tier coin system: Copper, Silver, Gold, Platinum
- Vendor buy/sell with charm-based pricing and Haggle skill discounts
- Ground loot rendered as clickable sprites

**Progression**
- 30 level cap with XP from combat (scaled by level difference)
- CP gains per level (10/15/20 at L1-10/11-20/21-30) for stat training via trainer NPC
- Equipment progression through 4 tiers: leather (L1) → chain/iron (L3) → enchanted (L5) → plate (L6)
- Inventory icon grid with item sprites and tap-to-use consumables
- Paperdoll equipment screen with slot-based equip/unequip

**Spells & Skills**
- 20 spells across 5 magic schools (Mage, Priest, Druid, Kai, Bard) — damage, heal, buff, DoT, HoT
- 12 skills: Bash, Kick, Backstab, Parry, Dodge, Hide, Sneak, Meditate, Perception, Pick Lock, Track, Haggle
- Spell bar with drag-to-assign slots and tap-to-ready auto-cast toggle
- Skill cooldown system — per-skill tick counters prevent spam
- Commands are validation-only; the game tick resolves everything uniformly

**Audio**
- Per-zone background music with crossfade on zone transitions
- Sound effects for combat, movement, spells, and item interactions
- Configurable volume controls for BGM and SFX independently

**Client**
- 10-direction navigation pad (cardinal, diagonal, vertical)
- Persistent minimap with clickable room navigation and fog-of-war
- Game log, character sheet, spell bar
- Kick direction picker and lock target picker UIs
- Dark fantasy stone UI theme

### What Needs Work

**Balance**
- Melee DPS is 3-5x higher than spell DPS — casters are underpowered
- Large difficulty jump between Forest (L1-3, max 5 dmg) and Marsh (L4-5, 15+ dmg) with no transitional content
- Some classes are barely viable solo (Priest has no damage spells and worst HP)
- Parry/Dodge proc rates are 5-6% at typical L1 stats — too low to notice
- Magic school level restrictions are not enforced — all classes with any school access can cast every spell in that school
- Several skill classRestriction mismatches in the data files
- No passive HP regeneration outside combat

**Multiplayer**
- Real-time WebSocket sessions work for basic gameplay
- Room-based chat, player presence, one session per account
- Player state persists across sessions
- **Needs significant stress testing** — concurrent combat, reconnection, edge cases

**Maker (World Editor)**
- Full CRUD for zones, rooms, NPCs, items, classes, races, skills, spells, loot tables
- Visual zone editor with drag-and-drop room placement and click-to-connect exits
- Default SFX editor and player sprite editor
- Import/export of `.nmd` bundles with default world auto-import
- AI generation pipeline exists but needs testing with real providers

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.3 (JVM 21) |
| Server | Ktor 3.4 + Netty |
| Database | SQLite + Exposed ORM |
| Client | Jetpack Compose + Material 3 |
| Images | Coil 3 (WebP with transparency) |
| Audio | Android MediaPlayer + SoundPool |
| Protocol | kotlinx.serialization over WebSocket |
| Shared Code | Kotlin Multiplatform |
| Build | Gradle 9.2 with configuration cache |
| Maker | React 19 + Express + Prisma 7 + SQLite |

## Running It

### Prerequisites
- JDK 21 (e.g., Amazon Corretto)
- Android SDK with platform 34+
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

### Tests
```bash
export JAVA_HOME=/path/to/jdk21
./gradlew :shared:jvmTest :server:test   # Server + shared tests
./gradlew :client:compileDebugKotlin     # Client compile check
cd maker && npm test                     # Maker tests
```

## Roadmap

### Near Term
- [ ] **Game Balance Pass** — address melee vs. magic disparity, Forest→Marsh difficulty gap, class viability
- [ ] **HP Regeneration** — passive HP regen outside combat so players don't have to buy potions constantly
- [x] **Spell School Level Enforcement** — classes should only cast spells at or below their school level
- [ ] **UX Polish** — zone danger warnings, trainer guidance for new players, vendor message improvements

### Medium Term
- [ ] **Transitional Content** — L3-4 zone or sub-zone to bridge Forest and Marsh difficulty
- [ ] **Quest System** — kill quests, fetch quests, quest log, rewards
- [ ] **NPC Dialogue** — conversation trees, quest givers, lore NPCs
- [ ] **Crafting System** — use the 5 crafting materials that currently drop with no purpose
- [ ] **Equipment Upgrades** — item rarity tiers, enchantments
- [ ] **Player Status Condensing** — compact HP/MP/XP bars for more game log space
- [ ] **Multiplayer Stress Testing** — concurrent players, combat interactions, reconnection

### Future Vision
- [ ] Boss encounters with special mechanics
- [ ] Party system with shared XP and group combat
- [ ] PvP — dueling, arenas, or PvP zones
- [ ] Guilds/clans
- [ ] Emotes and social features
- [ ] World events — timed spawns, invasions
- [ ] Web client
- [ ] iOS client

## AI Tooling

This project uses [Claude Code](https://claude.com/claude-code) for development, with custom agents and skills in the `.claude/` directory:

- **`/game-designer`** — launches an RPG systems design session to analyze balance, model combat math, and propose data file changes
- **`/playtest`** — launches an AI playtester that plays the game via WebSocket relay and files GitHub issues for bugs and UX problems
- Agent memory in `.claude/agent-memory/` persists balance audit findings and world state across sessions

## The Spirit of the Thing

This isn't a finished game. It's a living sketch — a place to experiment with what a MUD looks like when you can see it, when the protocol is type-safe, when the world data lives in version control alongside the code.

If you played MUDs in the '90s, you'll recognize the bones. If you didn't, maybe this will show you what all the fuss was about.
