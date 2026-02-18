package com.neomud.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.neomud.client.network.ConnectionState
import com.neomud.client.viewmodel.AuthState
import com.neomud.shared.NeoMudVersion

@Composable
fun LoginScreen(
    connectionState: ConnectionState,
    authState: AuthState,
    connectionError: String?,
    onConnect: (String, Int) -> Unit,
    onLogin: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onClearError: () -> Unit
) {
    var host by remember { mutableStateOf("10.0.2.2") }
    var port by remember { mutableStateOf("8080") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NeoMud",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = NeoMudVersion.VERSION,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (connectionState == ConnectionState.DISCONNECTED) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Server Host") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onConnect(host, port.toIntOrNull() ?: 8080)
                })
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onConnect(host, port.toIntOrNull() ?: 8080) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connect")
            }
        } else if (connectionState == ConnectionState.CONNECTING) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Connecting...")
        } else {
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (username.isNotBlank() && password.isNotBlank()) {
                        onLogin(username, password)
                    }
                })
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onLogin(username, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = authState !is AuthState.Loading && username.isNotBlank() && password.isNotBlank()
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Login")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text("Create Account")
            }
        }

        if (connectionError != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Connection failed: $connectionError",
                color = MaterialTheme.colorScheme.error
            )
        }

        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = authState.message,
                color = MaterialTheme.colorScheme.error
            )
            TextButton(onClick = onClearError) {
                Text("Dismiss")
            }
        }
    }
}
