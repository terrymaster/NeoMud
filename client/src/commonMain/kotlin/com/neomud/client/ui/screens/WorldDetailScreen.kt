package com.neomud.client.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.client.model.platform.WorldDetail
import com.neomud.client.model.platform.VersionDetail
import com.neomud.client.ui.theme.StoneTheme
import neomud.client.generated.resources.Res
import neomud.client.generated.resources.splash_forge
import org.jetbrains.compose.resources.painterResource

// ─────────────────────────────────────────────
// Palette
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
fun WorldDetailScreen(
    world: WorldDetail?,
    isLoading: Boolean,
    error: String?,
    onPlay: (WorldDetail) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Full-bleed splash background
        Image(
            painter = painterResource(Res.drawable.splash_forge),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
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
            Spacer(modifier = Modifier.height(12.dp))

            // ─── Top bar with back arrow ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button — stone circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Brush.radialGradient(listOf(StoneTheme.frameMid, StoneTheme.frameDark)),
                            CircleShape
                        )
                        .border(1.dp, StoneTheme.frameLight.copy(alpha = 0.5f), CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u2190", fontSize = 16.sp, color = BoneWhite)
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── Content ───
            when {
                isLoading -> {
                    Spacer(modifier = Modifier.height(60.dp))
                    CircularProgressIndicator(
                        color = BurnishedGold,
                        trackColor = AshGray.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Unrolling scroll...", fontSize = 12.sp, color = BoneWhite)
                }
                error != null -> {
                    Spacer(modifier = Modifier.height(60.dp))
                    Text(error, fontSize = 12.sp, color = CrimsonError, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(listOf(StoneTheme.frameLight, StoneTheme.frameDark)),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable(onClick = onRetry)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BoneWhite)
                    }
                }
                world != null -> {
                    WorldDetailContent(world = world, onPlay = onPlay)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.WorldDetailContent(
    world: WorldDetail,
    onPlay: (WorldDetail) -> Unit
) {
    val borderPx = 4.dp
    val hasServer = !world.serverEndpoint.isNullOrBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── World title ───
            Text(
                text = world.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BurnishedGold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Creator
            Text(
                text = "by ${world.creator.displayName}",
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = AshGray
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Gold ornamental line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, BurnishedGold.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ─── Server status ───
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Status dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (hasServer) VerdantGreen else AshGray.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasServer) "Server Online" else "Server Offline",
                    fontSize = 11.sp,
                    color = if (hasServer) VerdantGreen.copy(alpha = 0.8f) else AshGray
                )
            }

            // ─── Rating ───
            if (world.ratingCount > 0 && world.averageRating != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    StarRatingDisplay(rating = world.averageRating)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${world.ratingCount} ${if (world.ratingCount == 1) "rating" else "ratings"}",
                        fontSize = 11.sp,
                        color = AshGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ─── Description ───
            if (world.description.isNotBlank()) {
                Text(
                    text = world.description,
                    fontSize = 12.sp,
                    color = BoneWhite,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ─── Play button ───
            PlayButton(
                enabled = hasServer,
                onClick = { onPlay(world) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Runic divider ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, AshGray.copy(alpha = 0.4f))
                            )
                        )
                )
                Text(
                    text = " \u2726 ",
                    fontSize = 10.sp,
                    color = AshGray.copy(alpha = 0.5f)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(AshGray.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Version History ───
            Text(
                text = "Version History",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TorchAmber,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (world.versions.isEmpty()) {
                Text(
                    text = "No versions published yet.",
                    fontSize = 11.sp,
                    color = AshGray,
                    fontStyle = FontStyle.Italic
                )
            } else {
                world.versions.forEachIndexed { index, version ->
                    VersionRow(
                        version = version,
                        isLatest = index == 0
                    )
                    if (index < world.versions.lastIndex) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

// ─────────────────────────────────────────────
// Version row
// ─────────────────────────────────────────────
@Composable
private fun VersionRow(version: VersionDetail, isLatest: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(Color(0xFF14110E), DeepVoid)),
                RoundedCornerShape(4.dp)
            )
            .drawBehind {
                val w = size.width; val h = size.height
                drawLine(AshGray.copy(alpha = 0.15f), Offset(0f, 0f), Offset(w, 0f), 1f)
                drawLine(StoneTheme.innerShadow, Offset(0f, h - 1f), Offset(w, h - 1f), 1f)
                // Highlight bar for latest version
                if (isLatest) {
                    drawRect(
                        BurnishedGold.copy(alpha = 0.08f),
                        Offset.Zero,
                        Size(w, h)
                    )
                }
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "v${version.version}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLatest) BurnishedGold else BoneWhite
                    )
                    if (isLatest) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LATEST",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = VerdantGreen,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Text(
                    text = version.createdAt.take(10), // Just the date portion
                    fontSize = 10.sp,
                    color = AshGray
                )
            }

            if (version.changelog.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = version.changelog,
                    fontSize = 10.sp,
                    color = BoneWhite.copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Play button — prominent stone-gold action
// ─────────────────────────────────────────────
@Composable
private fun PlayButton(enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled)
        Brush.verticalGradient(listOf(BurnishedGold, Color(0xFF997733)))
    else
        Brush.verticalGradient(listOf(StoneTheme.frameMid, StoneTheme.frameDark))

    val textColor = if (enabled) DeepVoid else AshGray

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
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
        Text(
            text = if (enabled) "\u2694  Enter World  \u2694" else "Server Offline",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 1.sp
        )
    }
}
