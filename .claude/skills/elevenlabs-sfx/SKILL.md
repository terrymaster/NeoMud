---
name: elevenlabs-sfx
description: >
  Generate sound effects and background music for NeoMud using ElevenLabs AI. Use when creating
  or regenerating SFX for combat, spells, footsteps, NPC interactions, item pickups, ambient
  sounds, or BGM tracks. Handles generation and placement in the asset directory.
---

# ElevenLabs SFX & Music Generator

Generate sound effects and background music for NeoMud using ElevenLabs MCP tools and the maker's built-in generation endpoint.

---

## Generation Tools

### SFX: `text_to_sound_effects`

Use `mcp__elevenlabs-sfx__text_to_sound_effects` for short sound effects (0.5–5 seconds).

**Parameters:**
- `text` (required): Descriptive prompt for the sound effect
- `duration_seconds` (optional): 0.5–5.0 seconds (default varies by sound type)
- `output_format` (optional): `mp3_44100_128` (default)
- `loop` (optional): Whether the sound should loop seamlessly
- `output_directory` (optional): Where to save the file

### BGM: `compose_music`

Use `mcp__elevenlabs-sfx__compose_music` for background music tracks (10–300 seconds).

**Two modes:**

1. **Simple prompt** — pass a `prompt` string and optional `music_length_ms`:
   ```
   prompt: "Dark fantasy RPG dungeon theme, ominous drums, low strings"
   music_length_ms: 60000
   ```

2. **Composition plan** — structured multi-section composition with styles and durations. Use `create_composition_plan` (free, no credits) to draft the plan first, then pass it to `compose_music`.

**Composition plan structure:**
```json
{
  "positive_global_styles": ["dark fantasy", "orchestral"],
  "negative_global_styles": ["pop", "electronic", "vocals"],
  "sections": [
    {
      "section_name": "intro",
      "positive_local_styles": ["ominous", "building tension"],
      "negative_local_styles": ["upbeat"],
      "duration_ms": 15000,
      "lines": ["Deep war drums, low rumbling strings"]
    }
  ]
}
```

### Planning: `create_composition_plan` (FREE)

Use `mcp__elevenlabs-sfx__create_composition_plan` to draft a composition plan before generating. This does NOT cost credits — use it to iterate on the structure before committing to generation.

**Parameters:**
- `prompt` (required): Text description of desired music
- `music_length_ms` (optional): Target length 10000–300000ms
- `source_composition_plan` (optional): Existing plan to refine

### Maker Backend (alternative for SFX)

The maker's Express backend has a `/generate/sound` endpoint that calls ElevenLabs and saves directly:

```bash
curl -X POST http://localhost:5173/api/generate/sound \
  -H "Content-Type: application/json" \
  -d '{"prompt": "metallic sword clash", "duration": 2, "assetPath": "audio/items/sword_swing.mp3"}'
```

This requires the maker dev server to be running (`cd maker && npm run dev`).

---

## NeoMud SFX Conventions

### Directory Structure
```
maker/default_world_src/assets/audio/
├── bgm/              Background music (MP3, 1-2 MB, looping)
├── npcs/             NPC attack, miss, death, interact, exit sounds
├── items/            Weapon attack/miss, item use sounds
├── spells/           Spell cast, impact, miss sounds
├── rooms/            Room depart sounds, room effect sounds
└── general/          Shared sounds: dodge, parry, backstab, miss, loot_drop, etc.
```

### File Format
- **Format**: MP3 (`.mp3`)
- **SFX Duration**: 0.5–3 seconds typically
- **BGM Duration**: 30–120 seconds, looping

### Sound ID Naming
Sound IDs are referenced by name (no extension, no path prefix) in JSON data files:
- `sword_swing` → `audio/items/sword_swing.mp3` (weapon sounds go in `items/`)
- `wolf_bite` → `audio/npcs/wolf_bite.mp3` (NPC sounds go in `npcs/`)
- `fireball_cast` → `audio/spells/fireball_cast.mp3` (spell sounds go in `spells/`)
- `dodge` → `audio/general/dodge.mp3` (shared sounds go in `general/`)
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
4. Save to `maker/default_world_src/assets/audio/npcs/`

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
2. **Check** what already exists in `maker/default_world_src/assets/audio/{bgm,npcs,items,spells,rooms,general}/`
3. **Generate**:
   - SFX → `text_to_sound_effects` (short effects, 0.5–5s)
   - BGM → `create_composition_plan` (free draft) → `compose_music` (generate track, 10–300s)
4. **Rename** output files to match the expected naming convention
5. **Update** JSON data files with sound IDs if they're currently empty
6. **Rebuild** world bundle
