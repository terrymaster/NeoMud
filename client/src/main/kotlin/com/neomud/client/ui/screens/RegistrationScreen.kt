package com.neomud.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.neomud.client.viewmodel.AuthState
import com.neomud.shared.model.CharacterClassDef
import com.neomud.shared.model.RaceDef

@Composable
fun RegistrationScreen(
    authState: AuthState,
    availableClasses: List<CharacterClassDef>,
    availableRaces: List<RaceDef> = emptyList(),
    onRegister: (String, String, String, String, String) -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var characterName by rememberSaveable { mutableStateOf("") }
    var selectedClassId by rememberSaveable { mutableStateOf("WARRIOR") }
    var selectedRaceId by rememberSaveable { mutableStateOf("HUMAN") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(top = 32.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create Character",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = characterName,
            onValueChange = { characterName = it },
            label = { Text("Character Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (availableClasses.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            Text("Loading classes from server...", style = MaterialTheme.typography.bodySmall)
        } else {
            // Scrollable race + class list takes remaining space
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Race section
                if (availableRaces.isNotEmpty()) {
                    item {
                        Text("Choose Race:", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(availableRaces) { race ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRaceId == race.id,
                                onClick = { selectedRaceId = race.id }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(race.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    race.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                val m = race.statModifiers
                                val mods = listOf(
                                    "STR" to m.strength, "AGI" to m.agility, "INT" to m.intellect,
                                    "WIL" to m.willpower, "HLT" to m.health, "CHM" to m.charm
                                ).filter { it.second != 0 }
                                    .joinToString(" ") { "${it.first}:${if (it.second > 0) "+" else ""}${it.second}" }
                                if (mods.isNotEmpty()) {
                                    Text(
                                        mods,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                }
                                if (race.xpModifier != 1.0) {
                                    Text(
                                        "XP: ${(race.xpModifier * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }

                // Class section
                item {
                    Text("Choose Class:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                items(availableClasses) { cls ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedClassId == cls.id,
                            onClick = { selectedClassId = cls.id }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(cls.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                cls.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            val s = cls.baseStats
                            Text(
                                "STR:${s.strength} AGI:${s.agility} INT:${s.intellect} WIL:${s.willpower} HLT:${s.health} CHM:${s.charm}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Buttons pinned at bottom
        Button(
            onClick = { onRegister(username, password, characterName, selectedClassId, selectedRaceId) },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading &&
                    username.isNotBlank() && password.isNotBlank() && characterName.isNotBlank() &&
                    availableClasses.isNotEmpty()
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Register")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        TextButton(onClick = onBack) {
            Text("Back to Login")
        }

        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = authState.message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onClearError) {
                Text("Dismiss")
            }
        }
    }
}
