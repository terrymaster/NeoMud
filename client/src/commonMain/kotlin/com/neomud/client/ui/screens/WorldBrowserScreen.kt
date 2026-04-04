package com.neomud.client.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.client.model.platform.WorldSummary
import com.neomud.client.ui.theme.StoneTheme
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
private val VerdantGreen = Color(0xFF44CC55)

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
fun WorldBrowserScreen(
    worlds: List<WorldSummary>,
    isLoading: Boolean,
    error: String?,
    showDirectConnect: Boolean,
    onWorldClick: (String) -> Unit,
    onSearch: (String) -> Unit,
    onRetry: () -> Unit,
    onDirectConnect: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-bleed splash background
        Image(
            painter = painterResource(Res.drawable.splash_forge),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Dark scrim — slightly darker than login to emphasize cards
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ─── Title ───
            Text(
                text = "NeoMud",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = BurnishedGold,
                textAlign = TextAlign.Center
            )
            Text(
                text = NeoMudVersion.VERSION,
                fontSize = 10.sp,
                color = AshGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Gold ornamental line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, BurnishedGold.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ─── Section header ───
            Text(
                text = "\u2694  World Marketplace  \u2694",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TorchAmber,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ─── Search bar ───
            SearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                onSearch = {
                    focusManager.clearFocus()
                    onSearch(searchQuery)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ─── Content area ───
            when {
                isLoading -> {
                    Spacer(modifier = Modifier.height(40.dp))
                    CircularProgressIndicator(
                        color = BurnishedGold,
                        trackColor = AshGray.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Seeking worlds...", fontSize = 12.sp, color = BoneWhite)
                }
                error != null -> {
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(error, fontSize = 12.sp, color = CrimsonError, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))
                    RetryButton(onClick = onRetry)
                }
                worlds.isEmpty() -> {
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        "\u2736",
                        fontSize = 24.sp,
                        color = AshGray.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "No worlds found",
                        fontSize = 13.sp,
                        color = AshGray,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "The guild board is bare.\nCheck back soon, adventurer.",
                        fontSize = 11.sp,
                        color = AshGray.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(worlds, key = { it.id }) { world ->
                            WorldCard(world = world, onClick = { onWorldClick(world.slug) })
                        }
                        // Bottom padding
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }

            // ─── Direct Connect (dev builds only) ───
            if (showDirectConnect) {
                Spacer(modifier = Modifier.height(6.dp))
                // Runic divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, AshGray.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Direct Connect",
                    fontSize = 11.sp,
                    color = AshGray,
                    modifier = Modifier
                        .clickable(onClick = onDirectConnect)
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────
// World Card — quest poster pinned to the board
// ─────────────────────────────────────────────
@Composable
private fun WorldCard(
    world: WorldSummary,
    onClick: () -> Unit
) {
    val borderPx = 3.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind { drawStoneFrame(borderPx.toPx()) }
            .padding(borderPx)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF16130F), DeepVoid, Color(0xFF16130F))
                )
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top row: world name + version badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = world.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BurnishedGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // Version badge — wax seal style
                world.latestVersion?.let { v ->
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(StoneTheme.frameMid, StoneTheme.frameDark)
                                ),
                                CircleShape
                            )
                            .border(1.dp, StoneTheme.metalGold.copy(alpha = 0.6f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "v${v.version}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TorchAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Creator line
            Text(
                text = "by ${world.creator.displayName}",
                fontSize = 10.sp,
                fontStyle = FontStyle.Italic,
                color = AshGray
            )

            if (world.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = world.description,
                    fontSize = 11.sp,
                    color = BoneWhite.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
            }
        }

        // Subtle left accent bar (torch glow)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(2.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            BurnishedGold.copy(alpha = 0.3f),
                            BurnishedGold.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

// ─────────────────────────────────────────────
// Search field
// ─────────────────────────────────────────────
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 13.sp, color = BoneWhite),
        cursorBrush = SolidColor(BurnishedGold),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepVoid.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .border(1.dp, AshGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .drawBehind {
                        val w = size.width; val h = size.height
                        drawLine(StoneTheme.innerShadow, Offset(1f, h - 1f), Offset(w - 1f, h - 1f), 1f)
                        drawLine(StoneTheme.innerShadow, Offset(w - 1f, 1f), Offset(w - 1f, h - 1f), 1f)
                    }
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        "\uD83D\uDD0D  Search worlds...",
                        fontSize = 13.sp,
                        color = AshGray.copy(alpha = 0.5f)
                    )
                }
                innerTextField()
            }
        }
    )
}

// ─────────────────────────────────────────────
// Retry button
// ─────────────────────────────────────────────
@Composable
private fun RetryButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                Brush.verticalGradient(listOf(StoneTheme.frameLight, StoneTheme.frameDark)),
                RoundedCornerShape(4.dp)
            )
            .drawBehind {
                val w = size.width; val h = size.height
                drawLine(StoneTheme.frameLight.copy(alpha = 0.5f), Offset(0f, 0f), Offset(w, 0f), 1f)
                drawLine(Color.Black.copy(alpha = 0.5f), Offset(0f, h - 1f), Offset(w, h - 1f), 1f)
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BoneWhite)
    }
}
