package com.neomud.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.neomud.client.network.ConnectionState
import com.neomud.client.platform.defaultServerHost
import com.neomud.client.ui.theme.StoneTheme
import com.neomud.client.viewmodel.AuthState
import com.neomud.shared.NeoMudVersion
import neomud.client.generated.resources.Res
import neomud.client.generated.resources.splash_forge
import org.jetbrains.compose.resources.painterResource

// ─────────────────────────────────────────────
// Palette — shared medieval aesthetic
// ─────────────────────────────────────────────
private val DeepVoid = Color(0xFF080604)
private val WornLeather = Color(0xFF1A1510)
private val BurnishedGold = Color(0xFFCCA855)
private val TorchAmber = Color(0xFFBBA060)
private val BoneWhite = Color(0xFFD8CCAA)
private val AshGray = Color(0xFF5A5040)
private val CrimsonError = Color(0xFFCC4444)

// ─────────────────────────────────────────────
// Stone frame drawing
// ─────────────────────────────────────────────
private fun DrawScope.drawStoneFrame(borderPx: Float) {
    val w = size.width; val h = size.height
    drawRect(StoneTheme.frameMid, Offset.Zero, Size(w, borderPx))
    drawRect(StoneTheme.frameMid, Offset(0f, h - borderPx), Size(w, borderPx))
    drawRect(StoneTheme.frameMid, Offset(0f, borderPx), Size(borderPx, h - borderPx * 2))
    drawRect(StoneTheme.frameMid, Offset(w - borderPx, borderPx), Size(borderPx, h - borderPx * 2))
    drawLine(StoneTheme.frameLight, Offset(0f, 0f), Offset(w, 0f), 1f)
    drawLine(StoneTheme.frameLight, Offset(0f, 0f), Offset(0f, h), 1f)
    drawLine(StoneTheme.innerShadow, Offset(0f, h - 1f), Offset(w, h - 1f), 1f)
    drawLine(StoneTheme.innerShadow, Offset(w - 1f, 0f), Offset(w - 1f, h), 1f)
    drawLine(StoneTheme.innerShadow, Offset(borderPx, h - borderPx), Offset(w - borderPx, h - borderPx), 1f)
    drawLine(StoneTheme.innerShadow, Offset(w - borderPx, borderPx), Offset(w - borderPx, h - borderPx), 1f)
    drawLine(StoneTheme.runeGlow, Offset(borderPx, borderPx), Offset(w - borderPx, borderPx), 1f)
    drawLine(StoneTheme.runeGlow, Offset(borderPx, borderPx), Offset(borderPx, h - borderPx), 1f)
    val rivetRadius = 3f; val rivetOffset = borderPx / 2f
    drawCircle(StoneTheme.metalGold, rivetRadius, Offset(rivetOffset, rivetOffset))
    drawCircle(StoneTheme.metalGold, rivetRadius, Offset(w - rivetOffset, rivetOffset))
    drawCircle(StoneTheme.metalGold, rivetRadius, Offset(rivetOffset, h - rivetOffset))
    drawCircle(StoneTheme.metalGold, rivetRadius, Offset(w - rivetOffset, h - rivetOffset))
}

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
    var host by remember { mutableStateOf(defaultServerHost) }
    var port by remember { mutableStateOf("8080") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Splash background image (edge-to-edge, behind safe areas)
        Image(
            painter = painterResource(Res.drawable.splash_forge),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Dark scrim overlay for readability (50% to let forge glow bleed through)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )
        // Center card with stone frame (safe area padding keeps card clear of notch/home indicator)
        val borderPx = 4.dp
        Box(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 360.dp)
                .fillMaxWidth(0.9f)
                .drawBehind { drawStoneFrame(borderPx.toPx()) }
                .padding(borderPx)
                .background(
                    Brush.verticalGradient(
                        listOf(WornLeather, Color(0xFF100E0B), DeepVoid, Color(0xFF100E0B), WornLeather)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "NeoMud",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = BurnishedGold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = NeoMudVersion.VERSION,
                    fontSize = 11.sp,
                    color = AshGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Gold ornamental line
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, BurnishedGold.copy(alpha = 0.5f), Color.Transparent)
                            )
                        )
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (connectionState == ConnectionState.DISCONNECTED) {
                    // ─── Connection Phase ───
                    Text(
                        "Connect to Server",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TorchAmber
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    StoneTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = "Server Host",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    StoneTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = "Port",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            onConnect(host, port.toIntOrNull() ?: 8080)
                        })
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    StoneActionButton(
                        text = "Connect",
                        onClick = { onConnect(host, port.toIntOrNull() ?: 8080) }
                    )
                } else if (connectionState == ConnectionState.CONNECTING) {
                    // ─── Connecting ───
                    CircularProgressIndicator(
                        color = BurnishedGold,
                        trackColor = AshGray.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Connecting...", fontSize = 13.sp, color = BoneWhite)
                } else {
                    // ─── Authentication Phase ───
                    Text(
                        "Login",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TorchAmber
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    StoneTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    StoneTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        isPassword = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (username.isNotBlank() && password.isNotBlank()) {
                                onLogin(username, password)
                            }
                        })
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    StoneActionButton(
                        text = "Login",
                        onClick = { onLogin(username, password) },
                        enabled = authState !is AuthState.Loading && username.isNotBlank() && password.isNotBlank()
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                color = DeepVoid,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Create Account link
                    Text(
                        text = "Create Account",
                        fontSize = 13.sp,
                        color = BurnishedGold,
                        modifier = Modifier
                            .clickable(onClick = onNavigateToRegister)
                            .padding(vertical = 4.dp)
                    )
                }

                // Error display
                if (connectionError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Connection failed: $connectionError",
                        fontSize = 12.sp,
                        color = CrimsonError,
                        textAlign = TextAlign.Center
                    )
                }

                if (authState is AuthState.Error) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = authState.message,
                        fontSize = 12.sp,
                        color = CrimsonError,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dismiss",
                        fontSize = 12.sp,
                        color = AshGray,
                        modifier = Modifier
                            .clickable(onClick = onClearError)
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Stone-styled text field
// ─────────────────────────────────────────────
@Composable
private fun StoneTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            fontSize = 11.sp,
            color = AshGray,
            modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = BoneWhite
            ),
            cursorBrush = SolidColor(BurnishedGold),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepVoid, RoundedCornerShape(4.dp))
                        .border(1.dp, AshGray.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .drawBehind {
                            // Inner shadow for depth
                            val w = size.width; val h = size.height
                            drawLine(StoneTheme.innerShadow, Offset(1f, h - 1f), Offset(w - 1f, h - 1f), 1f)
                            drawLine(StoneTheme.innerShadow, Offset(w - 1f, 1f), Offset(w - 1f, h - 1f), 1f)
                        }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(
                            label,
                            fontSize = 14.sp,
                            color = AshGray.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

// ─────────────────────────────────────────────
// Stone action button
// ─────────────────────────────────────────────
@Composable
private fun StoneActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loadingContent: @Composable (() -> Unit)? = null
) {
    val bg = if (enabled)
        Brush.verticalGradient(listOf(BurnishedGold, Color(0xFF997733)))
    else
        Brush.verticalGradient(listOf(StoneTheme.frameMid, StoneTheme.frameDark))

    val textColor = if (enabled) DeepVoid else AshGray

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(bg, RoundedCornerShape(4.dp))
            .drawBehind {
                val w = size.width; val h = size.height
                if (enabled) {
                    drawLine(BurnishedGold.copy(alpha = 0.7f), Offset(0f, 0f), Offset(w, 0f), 1f)
                    drawLine(BurnishedGold.copy(alpha = 0.7f), Offset(0f, 0f), Offset(0f, h), 1f)
                } else {
                    drawLine(StoneTheme.frameLight.copy(alpha = 0.3f), Offset(0f, 0f), Offset(w, 0f), 1f)
                }
                drawLine(Color.Black.copy(alpha = 0.5f), Offset(0f, h - 1f), Offset(w, h - 1f), 1f)
                drawLine(Color.Black.copy(alpha = 0.5f), Offset(w - 1f, 0f), Offset(w - 1f, h), 1f)
            }
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
        loadingContent?.invoke()
    }
}
