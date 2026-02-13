package com.neomud.client.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neomud.shared.model.Direction

@Composable
fun DirectionPad(
    availableExits: Set<Direction>,
    onMove: (Direction) -> Unit,
    onLook: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonSize = Modifier.size(64.dp)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // North
        Button(
            onClick = { onMove(Direction.NORTH) },
            enabled = Direction.NORTH in availableExits,
            modifier = buttonSize,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("N")
        }

        Spacer(modifier = Modifier.height(4.dp))

        // West - Look - East
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onMove(Direction.WEST) },
                enabled = Direction.WEST in availableExits,
                modifier = buttonSize,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("W")
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(
                onClick = onLook,
                modifier = buttonSize,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Look")
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(
                onClick = { onMove(Direction.EAST) },
                enabled = Direction.EAST in availableExits,
                modifier = buttonSize,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("E")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // South
        Button(
            onClick = { onMove(Direction.SOUTH) },
            enabled = Direction.SOUTH in availableExits,
            modifier = buttonSize,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("S")
        }
    }
}
