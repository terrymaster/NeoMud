package com.neomud.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neomud.client.ui.components.DirectionPad
import com.neomud.client.ui.components.GameLog
import com.neomud.client.ui.components.MiniMap
import com.neomud.client.viewmodel.GameViewModel
import com.neomud.shared.model.Direction

@Composable
fun GameScreen(
    gameViewModel: GameViewModel
) {
    val roomInfo by gameViewModel.roomInfo.collectAsState()
    val mapData by gameViewModel.mapData.collectAsState()
    val gameLog by gameViewModel.gameLog.collectAsState()

    var sayText by remember { mutableStateOf("") }

    val availableExits = roomInfo?.room?.exits?.keys ?: emptySet()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top: Mini Map (~40%)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
        ) {
            val data = mapData
            if (data != null) {
                MiniMap(
                    rooms = data.rooms,
                    playerRoomId = data.playerRoomId
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)

        // Middle: Game Log (~40%)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
        ) {
            GameLog(entries = gameLog)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)

        // Bottom: Controls (~20%)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.2f)
                .padding(8.dp)
        ) {
            DirectionPad(
                availableExits = availableExits,
                onMove = { direction -> gameViewModel.move(direction) },
                onLook = { gameViewModel.look() },
                modifier = Modifier.weight(1f)
            )

            // Say bar
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = sayText,
                    onValueChange = { sayText = it },
                    placeholder = { Text("Say something...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = {
                        if (sayText.isNotBlank()) {
                            gameViewModel.say(sayText)
                            sayText = ""
                        }
                    },
                    enabled = sayText.isNotBlank()
                ) {
                    Text("Say")
                }
            }
        }
    }
}
