package com.neomud.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.client.ui.theme.MudColors
import com.neomud.shared.model.CharacterClassDef
import com.neomud.shared.model.SpellDef

private fun schoolDisplayName(school: String): String = school.replaceFirstChar { it.uppercase() }

private fun schoolColor(school: String): Color = when (school) {
    "mage" -> Color(0xFF5599FF)
    "priest" -> Color(0xFFFFDD44)
    "druid" -> Color(0xFF55CC55)
    "kai" -> Color(0xFFFF7744)
    "bard" -> Color(0xFFCC77FF)
    else -> MudColors.spell
}

@Composable
fun SpellPicker(
    slotIndex: Int,
    spells: List<SpellDef>,
    classDef: CharacterClassDef?,
    playerLevel: Int,
    onAssignSpell: (String?) -> Unit,
    onClose: () -> Unit
) {
    val schools = classDef?.magicSchools?.keys ?: emptySet()
    val filteredSpells = spells.filter { it.school in schools }
    val groupedBySchool = filteredSpells.groupBy { it.school }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* consume clicks */ },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Assign Spell to Slot ${slotIndex + 1}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MudColors.spell
                    )
                    TextButton(onClick = onClose) {
                        Text("X", color = Color(0xFFAAAAAA), fontSize = 18.sp)
                    }
                }

                HorizontalDivider(color = Color(0xFF333355), modifier = Modifier.padding(vertical = 8.dp))

                // Clear slot option
                TextButton(
                    onClick = { onAssignSpell(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Slot", color = Color(0xFFAAAAAA), fontSize = 14.sp)
                }

                HorizontalDivider(color = Color(0xFF333355), modifier = Modifier.padding(vertical = 4.dp))

                // Spell list grouped by school
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    groupedBySchool.forEach { (school, schoolSpells) ->
                        Text(
                            text = schoolDisplayName(school),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = schoolColor(school),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )

                        schoolSpells.sortedBy { it.levelRequired }.forEach { spell ->
                            val canCast = playerLevel >= spell.levelRequired
                            SpellRow(
                                spell = spell,
                                school = school,
                                enabled = canCast,
                                onClick = { if (canCast) onAssignSpell(spell.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpellRow(
    spell: SpellDef,
    school: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (enabled) Color(0xFFCCCCCC) else Color(0xFF666666)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // School icon
        Text(
            text = when (school) {
                "mage" -> "\u2728"
                "priest" -> "\u2721"
                "druid" -> "\uD83C\uDF3F"
                "kai" -> "\uD83D\uDD25"
                "bard" -> "\uD83C\uDFB5"
                else -> "\u2B50"
            },
            fontSize = 16.sp,
            modifier = Modifier.width(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Spell info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = spell.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = spell.description,
                fontSize = 11.sp,
                color = if (enabled) Color(0xFF999999) else Color(0xFF555555)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Level + MP cost
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "L${spell.levelRequired}",
                fontSize = 11.sp,
                color = if (enabled) Color(0xFFAAAAAA) else Color(0xFFFF5555),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${spell.manaCost} MP",
                fontSize = 11.sp,
                color = if (enabled) Color(0xFF88BBFF) else Color(0xFF555555)
            )
        }
    }
}
