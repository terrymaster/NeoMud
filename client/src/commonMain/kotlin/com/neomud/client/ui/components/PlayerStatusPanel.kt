package com.neomud.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.shared.model.ActiveEffect
import com.neomud.shared.model.EffectType
import com.neomud.shared.model.Player

@Composable
fun PlayerStatusPanel(
    player: Player,
    activeEffects: List<ActiveEffect>,
    isHidden: Boolean = false,
    isMeditating: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hpFraction = if (player.maxHp > 0) (player.currentHp.toFloat() / player.maxHp).coerceIn(0f, 1f) else 0f
    val hpColor = when {
        hpFraction > 0.5f -> Color(0xFF4CAF50)
        hpFraction > 0.25f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    val mpFraction = if (player.maxMp > 0) (player.currentMp.toFloat() / player.maxMp).coerceIn(0f, 1f) else 0f

    if (compact) {
        // Compact: effect icons on left, HP/MP bars with counts on right
        Row(
            modifier = modifier
                .clickable { onClick() }
                .padding(vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: effect icons (always reserve 18dp height for layout stability)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.height(14.dp)
            ) {
                if (isHidden) {
                    CompactEffectDot(Color(0xFF555555), MudIcons.Hidden)
                }
                if (isMeditating) {
                    CompactEffectDot(Color(0xFF7755CC), MudIcons.Meditating)
                }
                for (effect in activeEffects) {
                    val (bgColor, icon) = effectStyle(effect.type)
                    CompactEffectDot(bgColor, icon)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Right: HP and MP bars with labels and numeric counts
            Text("HP:", fontSize = 9.sp, color = Color(0xFFCCCCCC))
            Spacer(modifier = Modifier.width(2.dp))
            Text("${player.currentHp}/${player.maxHp}", fontSize = 9.sp, color = hpColor)
            Spacer(modifier = Modifier.width(2.dp))
            LinearProgressIndicator(
                progress = { hpFraction },
                modifier = Modifier.width(48.dp).height(6.dp),
                color = hpColor,
                trackColor = Color(0xFF333333),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("MP:", fontSize = 9.sp, color = Color(0xFFCCCCCC))
            Spacer(modifier = Modifier.width(2.dp))
            Text("${player.currentMp}/${player.maxMp}", fontSize = 9.sp, color = Color(0xFF448AFF))
            Spacer(modifier = Modifier.width(2.dp))
            LinearProgressIndicator(
                progress = { mpFraction },
                modifier = Modifier.width(48.dp).height(6.dp),
                color = Color(0xFF448AFF),
                trackColor = Color(0xFF333333),
            )
        }
    } else {
        Column(
            modifier = modifier
                .clickable { onClick() }
                .padding(vertical = 2.dp)
        ) {
            // HP bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "HP: ${player.currentHp}/${player.maxHp}",
                    fontSize = 10.sp,
                    color = Color(0xFFCCCCCC),
                    modifier = Modifier.width(72.dp)
                )
                LinearProgressIndicator(
                    progress = { hpFraction },
                    modifier = Modifier.weight(1f).height(8.dp),
                    color = hpColor,
                    trackColor = Color(0xFF333333),
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // MP bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "MP: ${player.currentMp}/${player.maxMp}",
                    fontSize = 10.sp,
                    color = Color(0xFFCCCCCC),
                    modifier = Modifier.width(72.dp)
                )
                LinearProgressIndicator(
                    progress = { mpFraction },
                    modifier = Modifier.weight(1f).height(8.dp),
                    color = Color(0xFF448AFF),
                    trackColor = Color(0xFF333333),
                )
            }

            // Active effect icons — always reserve space to prevent layout shift
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(18.dp)
            ) {
                if (isHidden) {
                    EffectDot(Color(0xFF555555), Color(0xFF888888), MudIcons.Hidden)
                }
                if (isMeditating) {
                    EffectDot(Color(0xFF7755CC), Color(0xFF9977EE), MudIcons.Meditating)
                }
                for (effect in activeEffects) {
                    EffectIcon(effect)
                }
            }
        }
    }
}

@Composable
private fun CompactEffectDot(color: Color, icon: ImageVector) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.8f))
            .border(1.dp, color, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(9.dp)
        )
    }
}

@Composable
private fun EffectDot(bgColor: Color, borderColor: Color, icon: ImageVector) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(bgColor.copy(alpha = 0.8f))
            .border(1.dp, borderColor, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp)
        )
    }
}

private fun effectStyle(type: EffectType): Pair<Color, ImageVector> = when (type) {
    EffectType.POISON -> Color(0xFF4CAF50) to MudIcons.EffectPoison
    EffectType.HEAL_OVER_TIME -> Color(0xFFE91E63) to MudIcons.EffectHealOverTime
    EffectType.BUFF_STRENGTH -> Color(0xFFF44336) to MudIcons.EffectBuffStrength
    EffectType.BUFF_AGILITY -> Color(0xFFFFEB3B) to MudIcons.EffectBuffAgility
    EffectType.BUFF_INTELLECT -> Color(0xFF2196F3) to MudIcons.EffectBuffIntellect
    EffectType.BUFF_WILLPOWER -> Color(0xFF9C27B0) to MudIcons.EffectBuffWillpower
    EffectType.HASTE -> Color(0xFF00BCD4) to MudIcons.EffectHaste
    EffectType.DAMAGE -> Color(0xFFFF5722) to MudIcons.EffectDamage
    EffectType.MANA_REGEN -> Color(0xFF3F51B5) to MudIcons.EffectManaRegen
    EffectType.MANA_DRAIN -> Color(0xFF7B1FA2) to MudIcons.EffectManaDrain
}

@Composable
private fun EffectIcon(effect: ActiveEffect) {
    val (bgColor, icon) = effectStyle(effect.type)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(bgColor.copy(alpha = 0.8f))
            .border(1.dp, bgColor, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = effect.type.name,
            tint = Color.White,
            modifier = Modifier.size(12.dp)
        )
    }
}
