---
name: elevenlabs-sfx
description: >
  Generate sound effects for NeoMud using ElevenLabs AI. Use when creating or regenerating
  SFX for combat, spells, footsteps, NPC interactions, item pickups, or ambient sounds.
  Handles generation and placement in the asset directory.
---

# ElevenLabs SFX Generator

Generate sound effects for NeoMud using the ElevenLabs `text_to_sound_effects` MCP tool and the maker's built-in SFX generation endpoint.

---

## Two Generation Paths

### Path 1: MCP Tool (Claude Code direct)

Use the `mcp__elevenlabs-sfx__text_to_sound_effects` tool to generate SFX directly. This saves files to the configured base path (`maker/default_world_src/assets/audio/`).

**Parameters:**
- `text` (required): Descriptive prompt for the sound effect
- `duration_seconds` (optional): 0.5–5.0 seconds (default varies by sound type)
- `output_format` (optional): `mp3_44100_128` (default)
- `loop` (optional): Whether the sound should loop seamlessly

### Path 2: Maker Backend

The maker's Express backend has a `/generate/sound` endpoint that calls ElevenLabs and saves directly:

```bash
curl -X POST http://localhost:5173/api/generate/sound \
  -H "Content-Type: application/json" \
  -d '{"prompt": "metallic sword clash", "duration": 2, "assetPath": "audio/sfx/sword_swing.mp3"}'
```

This requires the maker dev server to be running (`cd maker && npm run dev`).

---

## NeoMud SFX Conventions

### Directory Structure
```
maker/default_world_src/assets/audio/
├── bgm/              Background music (MP3, 1-2 MB, looping)
└── sfx/              Sound effects (MP3, 8-24 KB, short)
```

### File Format
- **Format**: MP3 (`.mp3`)
- **SFX Duration**: 0.5–3 seconds typically
- **BGM Duration**: 30–120 seconds, looping

### Sound ID Naming
Sound IDs are referenced by name (no extension, no path prefix) in JSON data files:
- `sword_swing` → `audio/sfx/sword_swing.mp3`
- `town_peaceful` → `audio/bgm/town_peaceful.mp3`

### Where Sounds Are Referenced

| Entity Type | JSON Fields | Location |
|-------------|-------------|----------|
| NPC | `attackSound`, `missSound`, `deathSound`, `interactSound` | Zone JSON files |
| Item | `attackSound`, `missSound`, `useSound` | `items.json` |
| Spell | `castSound`, `impactSound`, `missSound` | `spells.json` |
| Room | `departSound` | Zone JSON files |
| Room Effect | `sound` | Zone JSON files |
| Zone | `bgm` | Zone JSON files |

### Existing SFX Categories

**Combat** (melee):
- `sword_swing`, `sword_miss`, `miss`, `parry`, `dodge`, `backstab`

**Combat** (NPC-specific):
- `spider_bite`, `wolf_bite`, `claw_slash`, `claw_miss`
- `serpent_bite`, `serpent_miss`, `tongue_lash`, `tongue_miss`
- `rock_smash`, `rock_miss`, `magic_attack`, `magic_miss`
- `ghost_attack`, `ghost_miss`

**Deaths**:
- `enemy_death`, `spider_death`, `wolf_death`
- `creature_death`, `bandit_death`, `ghost_death`
- `golem_death`, `hag_death`

**Spells** (cast + hit pairs):
- `magic_missile_cast/hit`, `fireball_cast/hit`, `frost_bolt_cast/hit`
- `chi_strike_cast/hit`, `ki_blast_cast/hit`
- `spell_fizzle`, `healing_aura`

**Movement**:
- `footstep_cobblestone`, `footstep_dirt`, `footstep_grass`
- `footstep_marble`, `footstep_splash`, `footstep_wood`, `footstep_stone`

**Items/Loot**:
- `item_pickup`, `coin_pickup`, `potion_drink`, `loot_drop`

**Interactions**:
- `vendor_greet`

---

## Prompting Guide

ElevenLabs sound effects work best with descriptive, evocative prompts:

**Good prompts:**
- "Short metallic sword clash with ringing echo, fantasy game"
- "Magical energy bolt firing with crackling electricity, 8-bit inspired"
- "Heavy stone footstep on cobblestone, single step, dungeon atmosphere"
- "Glass potion bottle uncorking and liquid drinking gulp"
- "Creature death cry, reptilian screech fading out"
- "Ethereal healing chime with warm resonance"

**Bad prompts:**
- "sword" (too vague)
- "Make a sound for when the player attacks" (not descriptive)

**Tips:**
- Specify the material (metal, stone, wood, glass)
- Mention the mood (ominous, triumphant, mystical)
- Reference game audio style (fantasy RPG, retro, JRPG)
- Keep duration short (1-2s for combat, 0.5-1s for footsteps, 2-3s for spells)

---

## Workflow: Generating SFX for a New Entity

### For a new NPC:
1. Read the NPC definition from the zone JSON
2. Generate sounds for `attackSound`, `missSound`, `deathSound`
3. Add `interactSound` if the NPC is a vendor/trainer
4. Save to `maker/default_world_src/assets/audio/sfx/`

### For a new spell:
1. Read the spell definition from `spells.json`
2. Generate `castSound` and `impactSound`
3. Optionally generate a unique `missSound` (or reuse `spell_fizzle`)

### For a new zone:
1. Generate zone BGM → `audio/bgm/{zone_id}_{mood}.mp3`
2. Generate room `departSound` variants for different terrain
3. Generate any room effect sounds

### After generation:
- Run `cd maker && npm run rebuild-world` if the Vite server isn't running
- Run `./gradlew packageWorld` to rebuild the `.nmd` bundle (ensure JAVA_HOME is set)

---

## Session Flow

$ARGUMENTS

1. **Identify** what sounds are needed (read relevant JSON data files)
2. **Check** what SFX already exist in `maker/default_world_src/assets/audio/sfx/`
3. **Generate** missing sounds using `text_to_sound_effects` MCP tool
4. **Rename** MCP output files to match the expected naming convention
5. **Update** JSON data files with sound IDs if they're currently empty
6. **Rebuild** world bundle
