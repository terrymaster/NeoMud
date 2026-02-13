package com.neomud.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.neomud.client.viewmodel.AuthState
import com.neomud.shared.model.CharacterClass

@Composable
fun RegistrationScreen(
    authState: AuthState,
    onRegister: (String, String, String, CharacterClass) -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var characterName by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf(CharacterClass.WARRIOR) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create Character",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = characterName,
            onValueChange = { characterName = it },
            label = { Text("Character Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Choose Class:", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        CharacterClass.entries.forEach { cls ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedClass == cls,
                    onClick = { selectedClass = cls }
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(cls.name, style = MaterialTheme.typography.bodyLarge)
                    val s = cls.baseStats
                    Text(
                        "STR:${s.strength} DEX:${s.dexterity} CON:${s.constitution} INT:${s.intelligence} WIS:${s.wisdom} | HP:${s.maxHitPoints}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onRegister(username, password, characterName, selectedClass) },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading &&
                    username.isNotBlank() && password.isNotBlank() && characterName.isNotBlank()
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Register")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onBack) {
            Text("Back to Login")
        }

        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = authState.message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onClearError) {
                Text("Dismiss")
            }
        }
    }
}
