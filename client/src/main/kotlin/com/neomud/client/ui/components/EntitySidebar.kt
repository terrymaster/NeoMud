package com.neomud.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.shared.model.Npc

private val HostileColor = Color(0xFFFF6B35)
private val FriendlyColor = Color(0xFF4CAF50)
private val SelectedBg = Color(0x44FF6B35)

@Composable
fun EntitySidebar(
    entities: List<Npc>,
    selectedTargetId: String?,
    onSelectTarget: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .width(56.dp)
            .fillMaxHeight()
            .background(Color(0xFF1A1A2E)),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(entities, key = { it.id }) { npc ->
            val isSelected = npc.id == selectedTargetId
            val borderColor = if (npc.hostile) HostileColor else FriendlyColor

            Column(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .then(
                        if (isSelected) Modifier.background(SelectedBg, CircleShape)
                        else Modifier
                    )
                    .clickable(enabled = npc.hostile) {
                        if (npc.hostile) {
                            onSelectTarget(if (isSelected) null else npc.id)
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // NPC icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (isSelected) 3.dp else 2.dp,
                            color = borderColor,
                            shape = CircleShape
                        )
                        .background(Color(0xFF2A2A4A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (npc.hostile) "\u2694" else "\u263A",
                        fontSize = 18.sp,
                        color = borderColor
                    )
                }

                // HP bar for hostile NPCs
                if (npc.hostile && npc.maxHp > 0) {
                    val hpFraction = (npc.currentHp.toFloat() / npc.maxHp).coerceIn(0f, 1f)
                    val hpColor = when {
                        hpFraction > 0.5f -> Color(0xFF4CAF50)
                        hpFraction > 0.25f -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                    LinearProgressIndicator(
                        progress = { hpFraction },
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .padding(top = 2.dp),
                        color = hpColor,
                        trackColor = Color(0xFF333333),
                    )
                }

                // NPC name (truncated)
                Text(
                    text = npc.name,
                    fontSize = 8.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(52.dp)
                )
            }
        }
    }
}
