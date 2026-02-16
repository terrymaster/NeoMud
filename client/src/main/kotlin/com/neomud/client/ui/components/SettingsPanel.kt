package com.neomud.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.client.audio.AudioManager

private val CyanAccent = Color(0xFF55FFFF)
private val YellowAccent = Color(0xFFFFFF55)
private val DimText = Color(0xFFAAAAAA)

@Composable
fun SettingsPanel(
    isLandscape: Boolean,
    onSetLayoutPreference: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit,
    audioManager: AudioManager? = null
) {
    var masterVolume by remember { mutableFloatStateOf(audioManager?.masterVolume ?: 1f) }
    var sfxVolume by remember { mutableFloatStateOf(audioManager?.sfxVolume ?: 1f) }
    var bgmVolume by remember { mutableFloatStateOf(audioManager?.bgmVolume ?: 0.5f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* consume backdrop touches */ }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, CyanAccent, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            // Header row with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Settings",
                    color = CyanAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("X", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Layout section
            Text(
                "Layout",
                color = YellowAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Portrait button
                OutlinedButton(
                    onClick = { onSetLayoutPreference(false) },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (!isLandscape) 2.dp else 1.dp,
                        color = if (!isLandscape) CyanAccent else Color.Gray
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (!isLandscape) CyanAccent.copy(alpha = 0.15f) else Color.Transparent
                    )
                ) {
                    Text(
                        "Portrait",
                        color = if (!isLandscape) CyanAccent else DimText,
                        fontWeight = if (!isLandscape) FontWeight.Bold else FontWeight.Normal
                    )
                }
                // Landscape button
                OutlinedButton(
                    onClick = { onSetLayoutPreference(true) },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isLandscape) 2.dp else 1.dp,
                        color = if (isLandscape) CyanAccent else Color.Gray
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isLandscape) CyanAccent.copy(alpha = 0.15f) else Color.Transparent
                    )
                ) {
                    Text(
                        "Landscape",
                        color = if (isLandscape) CyanAccent else DimText,
                        fontWeight = if (isLandscape) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Audio section
            if (audioManager != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Audio",
                    color = YellowAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                VolumeSlider("Master", masterVolume) { value ->
                    masterVolume = value
                    audioManager.setVolumes(masterVolume, sfxVolume, bgmVolume)
                }
                VolumeSlider("SFX", sfxVolume) { value ->
                    sfxVolume = value
                    audioManager.setVolumes(masterVolume, sfxVolume, bgmVolume)
                }
                VolumeSlider("Music", bgmVolume) { value ->
                    bgmVolume = value
                    audioManager.setVolumes(masterVolume, sfxVolume, bgmVolume)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Divider before logout
            HorizontalDivider(color = Color(0xFF555555))
            Spacer(modifier = Modifier.height(12.dp))

            // Logout button
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFCC3333)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "Logout & Exit",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun VolumeSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = DimText,
            fontSize = 13.sp,
            modifier = Modifier.width(52.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = CyanAccent,
                activeTrackColor = CyanAccent,
                inactiveTrackColor = Color(0xFF333333)
            )
        )
        Text(
            "${(value * 100).toInt()}",
            color = DimText,
            fontSize = 13.sp,
            modifier = Modifier.width(32.dp)
        )
    }
}
