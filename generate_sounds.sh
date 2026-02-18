#!/usr/bin/env bash
# Generate all sound effects and BGM via ElevenLabs Sound Generation API
# Usage: ELEVENLABS_API_KEY=your_key ./generate_sounds.sh

set -euo pipefail

API_KEY="${ELEVENLABS_API_KEY:?Set ELEVENLABS_API_KEY environment variable}"
API_URL="https://api.elevenlabs.io/v1/sound-generation"

SFX_DIR="server/src/main/resources/assets/audio/sfx"
BGM_DIR="server/src/main/resources/assets/audio/bgm"

mkdir -p "$SFX_DIR" "$BGM_DIR"

FAIL_COUNT=0
SUCCESS_COUNT=0
SKIP_COUNT=0

generate() {
  local output_path="$1"
  local prompt="$2"
  local duration="$3"

  if [[ -f "$output_path" ]] && [[ $(stat -c%s "$output_path" 2>/dev/null || stat -f%z "$output_path" 2>/dev/null) -gt 1024 ]]; then
    echo "SKIP (exists): $output_path"
    SKIP_COUNT=$((SKIP_COUNT + 1))
    return 0
  fi

  echo -n "Generating: $output_path ... "

  local http_code
  http_code=$(curl -s -w "%{http_code}" -o "$output_path" \
    -X POST "$API_URL" \
    -H "xi-api-key: $API_KEY" \
    -H "Content-Type: application/json" \
    -d "{\"text\": \"$prompt\", \"duration_seconds\": $duration, \"prompt_influence\": 0.3, \"output_format\": \"ogg_48000\"}")

  if [[ "$http_code" == "200" ]] && [[ -f "$output_path" ]] && [[ $(stat -c%s "$output_path" 2>/dev/null || stat -f%z "$output_path" 2>/dev/null) -gt 1024 ]]; then
    echo "OK (HTTP $http_code)"
    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
  else
    echo "FAIL (HTTP $http_code)"
    rm -f "$output_path"
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi

  # Rate limit: small delay between calls
  sleep 0.5
}

echo "=== Generating SFX ==="

# Weapons (0.5s)
generate "$SFX_DIR/sword_swing.ogg"   "Sharp sword slash, metallic whoosh, short"                    0.5
generate "$SFX_DIR/sword_miss.ogg"    "Sword whooshing through empty air, quick miss"                0.5

# Combat (0.5-1.5s)
generate "$SFX_DIR/backstab.ogg"      "Dagger stab into flesh, sneaky strike"                       0.5
generate "$SFX_DIR/dodge.ogg"         "Quick dodge, leather rustle, near miss whoosh"                0.5
generate "$SFX_DIR/miss.ogg"          "Faint whoosh, attack missing"                                 0.5
generate "$SFX_DIR/enemy_death.ogg"   "Monster dying, pained groan and thud"                        1.5

# Items (0.5-1s)
generate "$SFX_DIR/potion_drink.ogg"  "Drinking potion, liquid gulp, glass bottle"                  1.0
generate "$SFX_DIR/loot_drop.ogg"     "Items dropping on stone, clinking metal"                     1.0
generate "$SFX_DIR/item_pickup.ogg"   "Picking up item, leather bag rustle"                         0.5
generate "$SFX_DIR/coin_pickup.ogg"   "Coins jingling, metal coins picked up"                       0.5

# NPC Combat (0.5-1.5s)
generate "$SFX_DIR/wolf_bite.ogg"     "Wolf bite and snarl, aggressive"                             1.0
generate "$SFX_DIR/wolf_miss.ogg"     "Wolf snapping jaws, missing, snarl"                          0.5
generate "$SFX_DIR/wolf_death.ogg"    "Wolf dying, pained whimper, collapse"                        1.5
generate "$SFX_DIR/spider_bite.ogg"   "Giant spider fangs striking, chitinous snap"                 1.0
generate "$SFX_DIR/spider_miss.ogg"   "Giant spider lunge miss, hissing"                            0.5
generate "$SFX_DIR/spider_death.ogg"  "Giant spider dying, screech and crunch"                      1.5

# Footsteps (0.5s)
generate "$SFX_DIR/footstep_cobblestone.ogg" "Single boot footstep on cobblestone"                  0.5
generate "$SFX_DIR/footstep_dirt.ogg"        "Single footstep on dirt, crunching earth"             0.5
generate "$SFX_DIR/footstep_grass.ogg"       "Single footstep on grass, soft rustle"                0.5
generate "$SFX_DIR/footstep_wood.ogg"        "Single footstep on wooden floor, creak"               0.5
generate "$SFX_DIR/footstep_marble.ogg"      "Single footstep on marble, echoing click"             0.5
generate "$SFX_DIR/footstep_splash.ogg"      "Single footstep splashing shallow water"              0.5

# Spell Casts (1s)
generate "$SFX_DIR/magic_missile_cast.ogg"  "Arcane energy charging and firing, magical zap"        1.0
generate "$SFX_DIR/frost_bolt_cast.ogg"     "Ice forming and launching, crystalline crack"          1.0
generate "$SFX_DIR/fireball_cast.ogg"       "Fire igniting, whooshing fireball launch"              1.0
generate "$SFX_DIR/arcane_shield_cast.ogg"  "Magical barrier forming, shimmering energy hum"        1.0
generate "$SFX_DIR/heal_cast.ogg"           "Warm healing light, gentle chime"                      1.0
generate "$SFX_DIR/blessing_cast.ogg"       "Divine blessing, holy choir note"                      1.0
generate "$SFX_DIR/divine_light_cast.ogg"   "Holy light burst, angelic tone"                        1.0
generate "$SFX_DIR/thorn_strike_cast.ogg"   "Thorny vines whipping and lashing"                     1.0
generate "$SFX_DIR/poison_cloud_cast.ogg"   "Toxic gas hissing, bubbling spread"                    1.0
generate "$SFX_DIR/natures_wrath_cast.ogg"  "Nature fury, rumbling earth and wind"                  1.0
generate "$SFX_DIR/inner_fire_cast.ogg"     "Internal fire igniting, resonant whoosh"               1.0
generate "$SFX_DIR/chi_strike_cast.ogg"     "Ki energy strike, sharp martial impact"                1.0
generate "$SFX_DIR/ki_blast_cast.ogg"       "Spiritual energy wave, deep pulse"                     1.0
generate "$SFX_DIR/diamond_body_cast.ogg"   "Body hardening to diamond, crystalline ring"           1.0
generate "$SFX_DIR/inspire_cast.ogg"        "Inspiring bardic lute chord"                           1.0
generate "$SFX_DIR/soothing_song_cast.ogg"  "Gentle soothing harp melody"                          1.0
generate "$SFX_DIR/discord_cast.ogg"        "Cacophonous blast, jarring dissonance"                 1.0
generate "$SFX_DIR/rallying_cry_cast.ogg"   "Mighty battle shout, rallying cry"                     1.0

# Spell Impacts (0.5s)
generate "$SFX_DIR/magic_missile_hit.ogg"   "Arcane bolt impact, magical pop"                       0.5
generate "$SFX_DIR/frost_bolt_hit.ogg"      "Ice shard shattering on impact"                        0.5
generate "$SFX_DIR/fireball_hit.ogg"        "Fireball explosion, fiery blast"                       0.5
generate "$SFX_DIR/thorn_strike_hit.ogg"    "Thorny vine pierce, snapping impact"                   0.5
generate "$SFX_DIR/poison_cloud_hit.ogg"    "Poison sizzle, acid contact"                           0.5
generate "$SFX_DIR/natures_wrath_hit.ogg"   "Nature force crash, thunderous impact"                 0.5
generate "$SFX_DIR/chi_strike_hit.ogg"      "Ki strike body impact, thud"                           0.5
generate "$SFX_DIR/ki_blast_hit.ogg"        "Energy wave hit, resonant impact"                      0.5
generate "$SFX_DIR/discord_hit.ogg"         "Discordant shockwave hit"                              0.5

# Spell Miss (1s)
generate "$SFX_DIR/spell_fizzle.ogg"        "Spell fizzling out, magic dissipating, faint crackle"  1.0

echo ""
echo "=== Generating BGM ==="

# BGM Tracks (22s max per ElevenLabs call)
# For full-length tracks, generate multiple segments and stitch with ffmpeg:
#   ffmpeg -i seg1.ogg -i seg2.ogg -filter_complex '[0][1]concat=n=2:v=0:a=1' output.ogg

generate "$BGM_DIR/town_peaceful.ogg" \
  "Medieval fantasy town ambiance, gentle lute melody in D major with wooden flute trills weaving through, distant market chatter and vendor calls, rhythmic blacksmith hammer clanging softly in background, horse hooves on cobblestone, children laughing in a square, warm golden afternoon atmosphere, peaceful and welcoming, rich harmonic layering with subtle hand drum percussion, loopable ambient music" \
  22.0

generate "$BGM_DIR/forest_danger.ogg" \
  "Dark enchanted forest tension music, tremolo strings building slow unease, deep cello drone with dissonant minor harmonics, distant wolf howl echoing through ancient trees, owl calls and rustling underbrush suggesting unseen movement, ominous low brass stabs punctuating silence, heartbeat-like timpani growing in intensity, high violin scrapes and creaking branches, atmospheric horror adventure tension, threatening and suspenseful, loopable dark ambient" \
  22.0

echo ""
echo "=== Summary ==="
echo "Success: $SUCCESS_COUNT"
echo "Skipped: $SKIP_COUNT"
echo "Failed:  $FAIL_COUNT"
echo "Total:   $((SUCCESS_COUNT + SKIP_COUNT + FAIL_COUNT))"

if [[ $FAIL_COUNT -gt 0 ]]; then
  echo ""
  echo "Re-run this script to retry failed files (existing files are skipped)."
  exit 1
fi
