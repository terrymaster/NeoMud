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
            .padding(if (isLandscape) 8.dp else 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (isLandscape) Modifier.fillMaxSize()
                    else Modifier.fillMaxWidth()
                )
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

            Spacer(modifier = Modifier.height(12.dp))

            if (isLandscape) {
                // Landscape: two-column layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left column: Layout + Logout
                    Column(modifier = Modifier.weight(1f)) {
                        LayoutSection(isLandscape, onSetLayoutPreference)
                        Spacer(modifier = Modifier.weight(1f))
                        LogoutButton(onLogout)
                    }

                    // Vertical divider
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(Color(0xFF555555))
                    )

                    // Right column: Audio
                    Column(modifier = Modifier.weight(1f)) {
                        if (audioManager != null) {
                            AudioSection(
                                masterVolume, sfxVolume, bgmVolume,
                                onMasterChange = { masterVolume = it; audioManager.setVolumes(masterVolume, sfxVolume, bgmVolume) },
                                onSfxChange = { sfxVolume = it; audioManager.setVolumes(masterVolume, sfxVolume, bgmVolume) },
                                onBgmChange = { bgmVolume = it; audioManager.setVolumes(masterVolume, sfxVolume, bgmVolume) }
                            )
                        }
                    }
                }
            } else {
                // Portrait: single-column vertical stack
                LayoutSection(isLandscape, onSetLayoutPreference)

                if (audioManager != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    AudioSection(
                        masterVolume, sfxVolume, bgmVolume,
                        onMasterChange = { masterVolume = it; audioManager.setVolumes(masterVolume, sfxVolume, bgmVolume) },
                        onSfxChange = { sfxVolume = it; audioManager.setVolumes(masterVolume, sfxVolume, bgmVolume) },
                        onBgmChange = { bgmVolume = it; audioManager.setVolumes(masterVolume, sfxVolume, bgmVolume) }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                LogoutButton(onLogout)
            }
        }
    }
}

@Composable
private fun LayoutSection(
    isLandscape: Boolean,
    onSetLayoutPreference: (Boolean) -> Unit
) {
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
}

@Composable
private fun AudioSection(
    masterVolume: Float,
    sfxVolume: Float,
    bgmVolume: Float,
    onMasterChange: (Float) -> Unit,
    onSfxChange: (Float) -> Unit,
    onBgmChange: (Float) -> Unit
) {
    Text(
        "Audio",
        color = YellowAccent,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    VolumeSlider("Master", masterVolume, onMasterChange)
    VolumeSlider("SFX", sfxVolume, onSfxChange)
    VolumeSlider("Music", bgmVolume, onBgmChange)
}

@Composable
private fun LogoutButton(onLogout: () -> Unit) {
    HorizontalDivider(color = Color(0xFF555555))
    Spacer(modifier = Modifier.height(12.dp))
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
