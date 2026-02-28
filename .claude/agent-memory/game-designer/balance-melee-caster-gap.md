# Melee vs Caster DPS Gap Analysis (Feb 2026)

## Key Finding
Melee DPS is 4-5x higher than caster DPS due to three factors:
1. STR contributes 1:1 to melee (spell stats divided by 3)
2. Melee fires every tick; spells have 2-4 tick cooldowns
3. Weapon bonuses apply to melee only

## Complete Damage Source Audit (All 5 Sources)

### Source 1: Basic Melee (CombatManager line 238-242)
- Formula: STR + weaponBonus + thresholdMeleeDmg + d(weaponRange)
- Fires every tick, no cooldown, no resource cost
- Proposed: STR/3 + weaponBonus + thresholdMeleeDmg + d(weaponRange)

### Source 2: Bash (CombatManager line 414)
- Formula: STR + weaponBonus + d(BASH_DAMAGE_RANGE=4)
- NOTE: Does NOT include thresholdMeleeDmg (minor inconsistency, intentional - bash valued for stun)
- 3 tick cooldown, 50% stun for 2 ticks
- Proposed: STR/3 + weaponBonus + d4

### Source 3: Kick (CombatManager line 480)
- Formula: STR/4 + AGI/4 + d(KICK_DAMAGE_RANGE=4)
- Already uses divisors, NOT affected by MELEE_STR_DIVISOR
- 2 tick cooldown, knockback + 2 tick stun
- Hardcoded /4 divisors should move to GameConfig (KICK_STR_DIVISOR, KICK_AGI_DIVISOR)

### Source 4: Backstab (CombatManager lines 249-252)
- NOT a separate formula - multiplies normal melee by BACKSTAB_DAMAGE_MULTIPLIER (3x)
- Automatically affected by STR divisor since it multiplies base melee
- One-time opener from stealth, breaks stealth on use
- Crit check happens BEFORE backstab: potential 4.5x multiplier (1.5 * 3)

### Source 5: Spells (SpellCommand line 67)
- Formula: basePower + stat/STAT_DIVISOR + level/LEVEL_DIVISOR + d(DICE_SIZE)
- Completely separate from melee formula, NOT affected by STR divisor
- Resource-gated (mana), cooldown-gated (2-4 ticks)

## Proposed Fix (Squeeze from Both Sides)
Target ratio: Warrior DPT ~1.3x Mage DPT

### Melee Nerfs
- Add MELEE_STR_DIVISOR = 3 to GameConfig.Combat
- Apply in CombatManager.kt melee formula AND bash formula
- Do NOT apply to kick (already has its own divisors)
- Compensate by increasing weapon damageBonus/damageRange values

### Spell Buffs
- SPELL_POWER_STAT_DIVISOR: 3 -> 2
- SPELL_POWER_LEVEL_DIVISOR: 2 -> 1
- SPELL_POWER_DICE_SIZE: 6 -> 8
- Reduce damage spell cooldowns by 1 across the board
- Increase tier 3 basePower (Fireball 30->45, Nature's Wrath 28->42)
- Reduce tier 1 mana costs slightly

### Weapon Stat Updates Needed
- Iron Sword: 3/6 -> 8/8
- Rustic Dagger: 2/4 -> 5/6
- Wooden Staff: 1/5 -> 3/6
- Short Bow: 2/5 -> 6/7
- Steel Longsword: 5/8 -> 12/10
- Mystic Staff: 7/6 -> 8/8
- Steel Greatsword: 8/10 -> 16/12

### Complete Before/After Table (L1, all sources)

Warrior (STR 35, Iron Sword):
- Melee: 41.5 -> 23.5 DPT (-43%)
- Bash amortized: 10.1 -> 5.4 DPT (-47%)
- Total sustained: 51.6 -> 28.9 DPT (-44%)

Thief (STR 15, Dagger):
- Melee: 19.5 -> 13.5 DPT (-31%)
- Backstab opener: 58.5 -> 40.5 (-31%)

Ninja (STR 18, Dagger):
- Melee: 22.5 -> 14.5 DPT (-36%)
- Kick: 14.5 -> 14.5 (unchanged)
- Backstab opener: 67.5 -> 43.5 (-36%)

Mage (INT 40, Magic Missile):
- Spell DPT: 9.5 -> 18.75 (+97%)
- Melee fallback: 10.0 -> 8.5 (-15%)
- Effective DPT (spell+melee alternating): ~10 -> ~23 (+130%)

Final ratio: Warrior 28.9 / Mage 23.0 = 1.26x (target was 1.3x)

### NPC Impact Assessment
- Forest mobs: Still die in 1-2 ticks to Warriors. No changes needed.
- Marsh mobs: Bog Toad (80HP) dies in 3 ticks post-nerf. Acceptable.
- Gorge mobs: Gorge Warden (220HP) takes ~7.5 ticks (11s). Consider 15-20% HP reduction (to ~180HP). Playtest first.

### Critical Dependency
STR divisor, weapon buffs, and spell buffs MUST ship as one atomic change.

### Code Cleanup Needed
- Kick: hardcoded /4 divisors -> GameConfig.Skills.KICK_STR_DIVISOR, KICK_AGI_DIVISOR
