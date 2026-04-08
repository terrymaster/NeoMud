package com.neomud.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import coil3.compose.LocalPlatformContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.neomud.client.ui.theme.StoneTheme
import com.neomud.shared.model.Coins
import com.neomud.shared.model.Item
import com.neomud.shared.model.RecipeInfo
import com.neomud.shared.protocol.ServerMessage

private val DeepVoid = Color(0xFF080604)
private val WornLeather = Color(0xFF1A1510)
private val BurnishedGold = Color(0xFFCCA855)
private val TorchAmber = Color(0xFFBBA060)
private val EmberOrange = Color(0xFFAA6B3A)
private val BoneWhite = Color(0xFFD8CCAA)
private val AshGray = Color(0xFF5A5040)
private val VerdantUpgrade = Color(0xFF44CC55)
private val CrimsonDowngrade = Color(0xFFCC4444)

private fun DrawScope.drawStoneFrame(borderPx: Float) {
    val w = size.width
    val h = size.height
    drawRect(StoneTheme.frameMid, Offset.Zero, Size(w, borderPx))
    drawRect(StoneTheme.frameMid, Offset(0f, h - borderPx), Size(w, borderPx))
    drawRect(StoneTheme.frameMid, Offset(0f, borderPx), Size(borderPx, h - borderPx * 2))
    drawRect(StoneTheme.frameMid, Offset(w - borderPx, borderPx), Size(borderPx, h - borderPx * 2))
    drawLine(StoneTheme.frameLight, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
    drawLine(StoneTheme.frameLight, Offset(0f, 0f), Offset(0f, h), strokeWidth = 1f)
    drawLine(StoneTheme.innerShadow, Offset(0f, h - 1f), Offset(w, h - 1f), strokeWidth = 1f)
    drawLine(StoneTheme.innerShadow, Offset(w - 1f, 0f), Offset(w - 1f, h), strokeWidth = 1f)
    drawLine(StoneTheme.innerShadow, Offset(borderPx, h - borderPx), Offset(w - borderPx, h - borderPx), strokeWidth = 1f)
    drawLine(StoneTheme.innerShadow, Offset(w - borderPx, borderPx), Offset(w - borderPx, h - borderPx), strokeWidth = 1f)
    drawLine(StoneTheme.runeGlow, Offset(borderPx, borderPx), Offset(w - borderPx, borderPx), strokeWidth = 1f)
    drawLine(StoneTheme.runeGlow, Offset(borderPx, borderPx), Offset(borderPx, h - borderPx), strokeWidth = 1f)
    val rivetRadius = 3f
    val rivetOffset = borderPx / 2f
    drawCircle(StoneTheme.metalGold, rivetRadius, Offset(rivetOffset, rivetOffset))
    drawCircle(StoneTheme.metalGold, rivetRadius, Offset(w - rivetOffset, rivetOffset))
    drawCircle(StoneTheme.metalGold, rivetRadius, Offset(rivetOffset, h - rivetOffset))
    drawCircle(StoneTheme.metalGold, rivetRadius, Offset(w - rivetOffset, h - rivetOffset))
}

@Composable
fun CraftingPanel(
    crafterInfo: ServerMessage.CraftingMenu,
    playerLevel: Int,
    itemCatalog: Map<String, Item>,
    onCraft: (String) -> Unit,
    onClose: () -> Unit
) {
    // Group recipes by category
    val categories = remember(crafterInfo.recipes) {
        crafterInfo.recipes
            .map { it.recipe.category }
            .distinct()
            .sorted()
    }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        val borderPx = 4.dp
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* prevent overlay dismiss when clicking panel content */ }
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
                    .padding(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = crafterInfo.crafterName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BurnishedGold
                    )
                    // Close button
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(StoneTheme.frameLight, StoneTheme.frameDark)
                                ),
                                RoundedCornerShape(4.dp)
                            )
                            .drawBehind {
                                drawLine(StoneTheme.frameLight, Offset(0f, 0f), Offset(size.width, 0f), 1f)
                                drawLine(StoneTheme.frameLight, Offset(0f, 0f), Offset(0f, size.height), 1f)
                                drawLine(StoneTheme.innerShadow, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f)
                                drawLine(StoneTheme.innerShadow, Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 1f)
                            }
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        CloseIcon(color = BoneWhite)
                    }
                }

                // Player coins
                Text(
                    text = "Your coins: ${crafterInfo.playerCoins.displayString()}",
                    fontSize = 13.sp,
                    color = BurnishedGold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Category tabs
                if (categories.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (cat in categories) {
                            CategoryTab(
                                label = cat.replaceFirstChar { it.uppercase() },
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Ornamental divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, BurnishedGold.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Recipe list
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    val filteredRecipes = crafterInfo.recipes.filter {
                        it.recipe.category == selectedCategory
                    }
                    if (filteredRecipes.isEmpty()) {
                        Text(
                            text = "No recipes in this category.",
                            fontSize = 13.sp,
                            color = AshGray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    for (recipeInfo in filteredRecipes) {
                        RecipeRow(
                            recipeInfo = recipeInfo,
                            playerLevel = playerLevel,
                            playerCoins = crafterInfo.playerCoins,
                            itemCatalog = itemCatalog,
                            onCraft = { onCraft(recipeInfo.recipe.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected)
        Brush.verticalGradient(listOf(StoneTheme.frameLight, StoneTheme.frameMid))
    else
        Brush.verticalGradient(listOf(StoneTheme.frameDark, Color(0xFF0D0A08)))

    Box(
        modifier = modifier
            .height(32.dp)
            .background(bg, RoundedCornerShape(4.dp))
            .drawBehind {
                val w = size.width; val h = size.height
                if (selected) {
                    drawLine(BurnishedGold.copy(alpha = 0.6f), Offset(0f, 0f), Offset(w, 0f), 1f)
                    drawLine(BurnishedGold.copy(alpha = 0.6f), Offset(0f, 0f), Offset(0f, h), 1f)
                } else {
                    drawLine(StoneTheme.frameLight.copy(alpha = 0.3f), Offset(0f, 0f), Offset(w, 0f), 1f)
                }
                drawLine(StoneTheme.innerShadow, Offset(0f, h - 1f), Offset(w, h - 1f), 1f)
                drawLine(StoneTheme.innerShadow, Offset(w - 1f, 0f), Offset(w - 1f, h), 1f)
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) BurnishedGold else AshGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecipeRow(
    recipeInfo: RecipeInfo,
    playerLevel: Int,
    playerCoins: Coins,
    itemCatalog: Map<String, Item>,
    onCraft: () -> Unit
) {
    val recipe = recipeInfo.recipe
    val outputItem = itemCatalog[recipe.outputItemId]
    val meetsLevel = playerLevel >= recipe.levelRequirement
    val canAfford = playerCoins.totalCopper() >= recipe.cost.totalCopper()
    val hasMaterials = recipeInfo.materialStatus.all { it.available >= it.required }
    val canCraft = recipeInfo.canCraft
    val context = LocalPlatformContext.current
    val serverBaseUrl = LocalServerBaseUrl.current

    val nameColor = when {
        !meetsLevel -> Color(0xFF666666)
        !canCraft -> AshGray
        else -> BoneWhite
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(
                Brush.horizontalGradient(listOf(Color(0xFF14110E), DeepVoid)),
                RoundedCornerShape(6.dp)
            )
            .drawBehind {
                val w = size.width; val h = size.height
                drawLine(AshGray.copy(alpha = 0.15f), Offset(0f, 0f), Offset(w, 0f), 1f)
                drawLine(StoneTheme.innerShadow, Offset(0f, h - 1f), Offset(w, h - 1f), 1f)
            }
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        // Top row: icon + name + craft button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Output item icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(DeepVoid, RoundedCornerShape(6.dp))
                    .border(1.dp, AshGray.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(spriteUrl(serverBaseUrl, recipe.outputItemId))
                        .crossfade(200)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = recipe.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(32.dp).padding(2.dp)
                )
            }
            Spacer(Modifier.width(8.dp))

            // Name + stats
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = recipe.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = nameColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (recipe.levelRequirement > 1) {
                        Text(
                            text = " Lv${recipe.levelRequirement}",
                            fontSize = 11.sp,
                            color = if (meetsLevel) AshGray else CrimsonDowngrade
                        )
                    }
                }
                if (outputItem != null) {
                    val statsText = buildString {
                        if (outputItem.slot.isNotEmpty()) append(outputItem.slot.replaceFirstChar { it.uppercase() })
                        if (outputItem.armorValue > 0) append(" | ARM ${outputItem.armorValue}")
                        if (outputItem.damageBonus > 0) append(" | DMG +${outputItem.damageBonus}")
                        if (outputItem.damageRange > 0) append(" (1-${outputItem.damageRange})")
                    }
                    if (statsText.isNotEmpty()) {
                        Text(text = statsText, fontSize = 11.sp, color = TorchAmber)
                    }
                }
            }

            // Craft button
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .background(
                        if (canCraft)
                            Brush.verticalGradient(listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)))
                        else
                            Brush.verticalGradient(listOf(StoneTheme.frameDark, Color(0xFF0D0A08))),
                        RoundedCornerShape(4.dp)
                    )
                    .drawBehind {
                        val w = size.width; val h = size.height
                        if (canCraft) {
                            drawLine(VerdantUpgrade.copy(alpha = 0.5f), Offset(0f, 0f), Offset(w, 0f), 1f)
                            drawLine(VerdantUpgrade.copy(alpha = 0.5f), Offset(0f, 0f), Offset(0f, h), 1f)
                        }
                        drawLine(Color.Black.copy(alpha = 0.5f), Offset(0f, h - 1f), Offset(w, h - 1f), 1f)
                        drawLine(Color.Black.copy(alpha = 0.5f), Offset(w - 1f, 0f), Offset(w - 1f, h), 1f)
                    }
                    .then(if (canCraft) Modifier.clickable(onClick = onCraft) else Modifier)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Craft", fontSize = 12.sp,
                    color = if (canCraft) Color.White else AshGray,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Materials row
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Needs:", fontSize = 11.sp, color = AshGray)
            for (mat in recipeInfo.materialStatus) {
                val hasEnough = mat.available >= mat.required
                Text(
                    text = "${mat.itemName} ${mat.available}/${mat.required}",
                    fontSize = 11.sp,
                    color = if (hasEnough) VerdantUpgrade else CrimsonDowngrade
                )
            }
        }

        // Cost row (if non-zero)
        if (recipe.cost.totalCopper() > 0) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fee: ", fontSize = 11.sp, color = AshGray)
                Text(
                    text = recipe.cost.displayString(),
                    fontSize = 11.sp,
                    color = if (canAfford) BurnishedGold else CrimsonDowngrade
                )
            }
        }
    }
}
