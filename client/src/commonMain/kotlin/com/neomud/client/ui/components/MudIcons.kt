package com.neomud.client.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centralized icon registry for the MUD client.
 * All UI icons are Material Icons ImageVectors — guaranteed cross-platform.
 */
object MudIcons {

    // ── Toolbar ──
    val Inventory: ImageVector = Icons.Filled.Inventory2
    val Equipment: ImageVector = Icons.Filled.Shield
    val Map: ImageVector = Icons.Filled.Map
    val Settings: ImageVector = Icons.Filled.Settings

    // ── Skills ──
    val Bash: ImageVector = Icons.Filled.SportsMma
    val Kick: ImageVector = Icons.Filled.DoNotStep
    val Sneak: ImageVector = Icons.Filled.VisibilityOff
    val Meditate: ImageVector = Icons.Filled.SelfImprovement
    val Track: ImageVector = Icons.Filled.Pets
    val PickLock: ImageVector = Icons.Filled.LockOpen
    val Rest: ImageVector = Icons.Filled.DarkMode
    val Attack: ImageVector = Icons.Filled.SportsMma

    // ── Status Effects ──
    val EffectPoison: ImageVector = Icons.Filled.Science
    val EffectHealOverTime: ImageVector = Icons.Filled.Favorite
    val EffectBuffStrength: ImageVector = Icons.Filled.Hardware
    val EffectBuffAgility: ImageVector = Icons.Filled.FlashOn
    val EffectBuffIntellect: ImageVector = Icons.AutoMirrored.Filled.MenuBook
    val EffectBuffWillpower: ImageVector = Icons.Filled.Psychology
    val EffectHaste: ImageVector = Icons.Filled.Speed
    val EffectDamage: ImageVector = Icons.Filled.Whatshot
    val EffectManaRegen: ImageVector = Icons.Filled.WaterDrop
    val EffectManaDrain: ImageVector = Icons.Filled.InvertColors

    // ── State Indicators ──
    val Hidden: ImageVector = Icons.Filled.VisibilityOff
    val Meditating: ImageVector = Icons.Filled.SelfImprovement

    // ── Room NPCs ──
    val Vendor: ImageVector = Icons.Filled.AttachMoney
    val Trainer: ImageVector = Icons.AutoMirrored.Filled.MenuBook

    // ── Room Overlay / Interactables ──
    val ExitOpen: ImageVector = Icons.Filled.LockOpen
    val TreasureDrop: ImageVector = Icons.Filled.WorkspacePremium
    val MonsterSpawn: ImageVector = Icons.Filled.Warning
    val RoomEffect: ImageVector = Icons.Filled.AutoAwesome
    val Teleport: ImageVector = Icons.Filled.Cyclone
    val InteractDefault: ImageVector = Icons.Filled.Settings

    // ── NPC Context Menu ──
    val TrackNpc: ImageVector = Icons.Filled.Pets
    val KickNpc: ImageVector = Icons.Filled.DoNotStep

    // ── Spell Schools ──
    val SchoolMage: ImageVector = Icons.Filled.AutoAwesome
    val SchoolPriest: ImageVector = Icons.Filled.Brightness7
    val SchoolDruid: ImageVector = Icons.Filled.Grass
    val SchoolKai: ImageVector = Icons.Filled.Whatshot
    val SchoolBard: ImageVector = Icons.Filled.MusicNote
    val SchoolDefault: ImageVector = Icons.Filled.Star

    // ── Per-Spell Icons ──
    // Mage
    val SpellMagicMissile: ImageVector = Icons.Filled.AutoAwesome
    val SpellArcaneShield: ImageVector = Icons.Filled.Shield
    val SpellFrostBolt: ImageVector = Icons.Filled.AcUnit
    val SpellFireball: ImageVector = Icons.Filled.Whatshot
    // Priest
    val SpellSmite: ImageVector = Icons.Filled.WbSunny
    val SpellHolySmite: ImageVector = Icons.Filled.LightMode
    val SpellMinorHeal: ImageVector = Icons.Filled.Favorite
    val SpellBlessing: ImageVector = Icons.Filled.FrontHand
    val SpellCureWounds: ImageVector = Icons.Filled.VolunteerActivism
    val SpellDivineLight: ImageVector = Icons.Filled.Brightness7
    // Druid
    val SpellThornStrike: ImageVector = Icons.Filled.Park
    val SpellHealingTouch: ImageVector = Icons.Filled.Grass
    val SpellPoisonCloud: ImageVector = Icons.Filled.Science
    val SpellNaturesWrath: ImageVector = Icons.Filled.FlashOn
    // Kai
    val SpellInnerFire: ImageVector = Icons.Filled.Whatshot
    val SpellChiStrike: ImageVector = Icons.Filled.SportsMma
    val SpellKiBlast: ImageVector = Icons.Filled.Flare
    val SpellDiamondBody: ImageVector = Icons.Filled.Diamond
    // Bard
    val SpellCuttingWords: ImageVector = Icons.Filled.RecordVoiceOver
    val SpellInspire: ImageVector = Icons.Filled.EmojiEvents
    val SpellSoothingSong: ImageVector = Icons.Filled.GraphicEq
    val SpellDiscord: ImageVector = Icons.Filled.MusicOff
    val SpellRallyingCry: ImageVector = Icons.Filled.Campaign

    /** Map skill IDs to their icons */
    fun skillIcon(skillId: String): ImageVector = when (skillId) {
        "BASH" -> Bash
        "KICK" -> Kick
        "SNEAK" -> Sneak
        "MEDITATE" -> Meditate
        "TRACK" -> Track
        "PICK_LOCK" -> PickLock
        "REST" -> Rest
        else -> Attack
    }

    /** Map spell IDs to their unique icons */
    fun spellIcon(spellId: String): ImageVector = when (spellId) {
        "MAGIC_MISSILE" -> SpellMagicMissile
        "ARCANE_SHIELD" -> SpellArcaneShield
        "FROST_BOLT" -> SpellFrostBolt
        "FIREBALL" -> SpellFireball
        "SMITE" -> SpellSmite
        "HOLY_SMITE" -> SpellHolySmite
        "MINOR_HEAL" -> SpellMinorHeal
        "BLESSING" -> SpellBlessing
        "CURE_WOUNDS" -> SpellCureWounds
        "DIVINE_LIGHT" -> SpellDivineLight
        "THORN_STRIKE" -> SpellThornStrike
        "HEALING_TOUCH" -> SpellHealingTouch
        "POISON_CLOUD" -> SpellPoisonCloud
        "NATURES_WRATH" -> SpellNaturesWrath
        "INNER_FIRE" -> SpellInnerFire
        "CHI_STRIKE" -> SpellChiStrike
        "KI_BLAST" -> SpellKiBlast
        "DIAMOND_BODY" -> SpellDiamondBody
        "CUTTING_WORDS" -> SpellCuttingWords
        "INSPIRE" -> SpellInspire
        "SOOTHING_SONG" -> SpellSoothingSong
        "DISCORD" -> SpellDiscord
        "RALLYING_CRY" -> SpellRallyingCry
        else -> SchoolDefault
    }

    /** Map spell school to its icon */
    fun schoolIcon(school: String): ImageVector = when (school) {
        "mage" -> SchoolMage
        "priest" -> SchoolPriest
        "druid" -> SchoolDruid
        "kai" -> SchoolKai
        "bard" -> SchoolBard
        else -> SchoolDefault
    }
}
