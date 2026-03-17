package com.neomud.client.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.client.ui.theme.MudColors
import com.neomud.shared.model.SpellDef
import com.neomud.shared.model.TargetType

/** School-level icon — used for empty slots and school headers */
fun schoolIcon(school: String): String = when (school) {
    "mage" -> "\u2728"   // ✨ sparkles
    "priest" -> "\u2721" // ✡ star of david (holy)
    "druid" -> "\uD83C\uDF3F" // 🌿 herb
    "kai" -> "\uD83D\uDD25"   // 🔥 fire
    "bard" -> "\uD83C\uDFB5" // 🎵 musical note
    else -> "\u2B50"     // ⭐ star
}

/** Per-spell unique icon */
fun spellIcon(spellId: String): String = when (spellId) {
    // Mage
    "MAGIC_MISSILE" -> "\uD83D\uDD2E"   // 🔮 crystal ball
    "ARCANE_SHIELD" -> "\uD83D\uDEE1\uFE0F" // 🛡️ shield
    "FROST_BOLT"    -> "\u2744\uFE0F"    // ❄️ snowflake
    "FIREBALL"      -> "\uD83D\uDD25"    // 🔥 fire
    // Priest
    "SMITE"         -> "\u2600\uFE0F"    // ☀️ sun
    "HOLY_SMITE"    -> "\u271D\uFE0F"    // ✝️ cross
    "MINOR_HEAL"    -> "\uD83D\uDC9A"    // 💚 green heart
    "BLESSING"      -> "\uD83D\uDE4F"    // 🙏 folded hands
    "CURE_WOUNDS"   -> "\uD83D\uDC96"    // 💖 sparkling heart
    "DIVINE_LIGHT"  -> "\uD83C\uDF1F"    // 🌟 glowing star
    // Druid
    "THORN_STRIKE"  -> "\uD83C\uDF39"    // 🌹 rose
    "HEALING_TOUCH" -> "\uD83C\uDF43"    // 🍃 leaf
    "POISON_CLOUD"  -> "\u2620\uFE0F"    // ☠️ skull crossbones
    "NATURES_WRATH" -> "\u26A1"          // ⚡ lightning
    // Kai
    "INNER_FIRE"    -> "\uD83D\uDD25"    // 🔥 fire (ki flame)
    "CHI_STRIKE"    -> "\uD83D\uDC4A"    // 👊 fist
    "KI_BLAST"      -> "\uD83D\uDCAB"    // 💫 dizzy star
    "DIAMOND_BODY"  -> "\uD83D\uDC8E"    // 💎 gem
    // Bard
    "CUTTING_WORDS" -> "\uD83D\uDDE3\uFE0F" // 🗣️ speaking head
    "INSPIRE"       -> "\uD83D\uDCEF"    // 📯 postal horn
    "SOOTHING_SONG" -> "\uD83C\uDFB6"    // 🎶 notes
    "DISCORD"       -> "\uD83D\uDCA5"    // 💥 explosion
    "RALLYING_CRY"  -> "\uD83D\uDCE2"    // 📢 loudspeaker
    else            -> "\u2B50"           // ⭐ fallback
}

fun schoolColor(school: String): Color = when (school) {
    "mage" -> Color(0xFF5599FF)
    "priest" -> Color(0xFFFFDD44)
    "druid" -> Color(0xFF55CC55)
    "kai" -> Color(0xFFFF7744)
    "bard" -> Color(0xFFCC77FF)
    else -> MudColors.spell
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpellBar(
    spellSlots: List<String?>,
    spellCatalog: Map<String, SpellDef>,
    readiedSpellId: String?,
    currentMp: Int,
    onReadySpell: (Int) -> Unit,
    onOpenSpellPicker: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        spellSlots.forEachIndexed { index, spellId ->
            val spell = spellId?.let { spellCatalog[it] }
            val isReadied = spell != null && spellId == readiedSpellId
            val hasEnoughMp = spell == null || currentMp >= spell.manaCost

            val borderColor = when {
                isReadied -> MudColors.spell
                spell != null && hasEnoughMp -> schoolColor(spell.school)
                spell != null -> Color.Gray
                else -> Color(0xFF555555)
            }
            val bgColor = when {
                isReadied -> Color(0x449B59FF)
                else -> Color.Transparent
            }

            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .combinedClickable(
                        onClick = {
                            if (spell != null) {
                                onReadySpell(index)
                            } else {
                                onOpenSpellPicker(index)
                            }
                        },
                        onLongClick = { onOpenSpellPicker(index) }
                    ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    width = if (isReadied) 2.dp else 1.dp,
                    color = borderColor
                ),
                color = bgColor
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (spell != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = spellIcon(spell.id),
                                fontSize = 13.sp,
                                color = if (hasEnoughMp) borderColor else Color.Gray
                            )
                            Text(
                                text = "${spell.manaCost}",
                                fontSize = 8.sp,
                                color = if (hasEnoughMp) Color(0xFF88BBFF) else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Empty slot — show school icon if available, otherwise slot number
                        Text(
                            text = schoolIcon(""),
                            fontSize = 14.sp,
                            color = Color(0xFF555555),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
