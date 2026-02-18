package com.neomud.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.neomud.client.viewmodel.AuthState
import com.neomud.shared.model.CharacterClassDef
import com.neomud.shared.model.RaceDef
import com.neomud.shared.model.StatAllocator
import com.neomud.shared.model.Stats

@Composable
fun RegistrationScreen(
    authState: AuthState,
    availableClasses: List<CharacterClassDef>,
    availableRaces: List<RaceDef> = emptyList(),
    serverBaseUrl: String = "",
    onRegister: (String, String, String, String, String, String, Stats) -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var characterName by rememberSaveable { mutableStateOf("") }
    var selectedGender by rememberSaveable { mutableStateOf("neutral") }
    var selectedRaceId by rememberSaveable { mutableStateOf("HUMAN") }
    var selectedClassId by rememberSaveable { mutableStateOf("WARRIOR") }
    var allocatedStats by remember { mutableStateOf<Stats?>(null) }

    val selectedClass = availableClasses.find { it.id == selectedClassId }
    val selectedRace = availableRaces.find { it.id == selectedRaceId }

    // Compute effective minimums: class mins + race mods, floored at 1
    fun effectiveMin(classDef: CharacterClassDef?, raceDef: RaceDef?): Stats {
        val base = classDef?.minimumStats ?: return Stats()
        val mods = raceDef?.statModifiers ?: return base
        return Stats(
            strength = maxOf(1, base.strength + mods.strength),
            agility = maxOf(1, base.agility + mods.agility),
            intellect = maxOf(1, base.intellect + mods.intellect),
            willpower = maxOf(1, base.willpower + mods.willpower),
            health = maxOf(1, base.health + mods.health),
            charm = maxOf(1, base.charm + mods.charm)
        )
    }

    // Initialize allocated stats when class or race changes
    LaunchedEffect(selectedClassId, selectedRaceId, availableClasses, availableRaces) {
        if (selectedClass != null) {
            allocatedStats = effectiveMin(selectedClass, selectedRace)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create Character",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Step indicator dots
        StepIndicator(currentStep = currentStep, totalSteps = 6)

        Spacer(modifier = Modifier.height(12.dp))

        // Step content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (currentStep) {
                0 -> CredentialsStep(
                    username = username,
                    password = password,
                    characterName = characterName,
                    onUsernameChange = { username = it },
                    onPasswordChange = { password = it },
                    onCharacterNameChange = { characterName = it }
                )
                1 -> GenderSelectionStep(
                    selectedGender = selectedGender,
                    onGenderSelected = { selectedGender = it }
                )
                2 -> RaceSelectionStep(
                    availableRaces = availableRaces,
                    selectedRaceId = selectedRaceId,
                    onRaceSelected = { selectedRaceId = it }
                )
                3 -> ClassSelectionStep(
                    availableClasses = availableClasses,
                    selectedClassId = selectedClassId,
                    onClassSelected = {
                        selectedClassId = it
                        // Reset allocation when class changes
                        val cls = availableClasses.find { c -> c.id == it }
                        if (cls != null) allocatedStats = effectiveMin(cls, selectedRace)
                    }
                )
                4 -> {
                    val effMin = effectiveMin(selectedClass, selectedRace)
                    StatAllocationStep(
                        effectiveMinimum = effMin,
                        allocatedStats = allocatedStats ?: effMin,
                        onStatsChanged = { allocatedStats = it }
                    )
                }
                5 -> CharacterPreviewStep(
                    characterName = characterName,
                    selectedRace = selectedRace,
                    selectedClass = selectedClass,
                    selectedGender = selectedGender,
                    allocatedStats = allocatedStats ?: effectiveMin(selectedClass, selectedRace),
                    serverBaseUrl = serverBaseUrl
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }
            } else {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back to Login")
                }
            }

            if (currentStep < 5) {
                val canAdvance = when (currentStep) {
                    0 -> username.isNotBlank() && password.isNotBlank() && characterName.isNotBlank()
                    1 -> true // Gender always has a default
                    2 -> availableRaces.isNotEmpty()
                    3 -> availableClasses.isNotEmpty()
                    4 -> {
                        val effMin = effectiveMin(selectedClass, selectedRace)
                        val stats = allocatedStats ?: effMin
                        StatAllocator.totalCpUsed(stats, effMin) == StatAllocator.CP_POOL
                    }
                    else -> true
                }
                Button(
                    onClick = { currentStep++ },
                    modifier = Modifier.weight(1f),
                    enabled = canAdvance
                ) {
                    Text("Next")
                }
            } else {
                val effMin = effectiveMin(selectedClass, selectedRace)
                val stats = allocatedStats ?: effMin
                val cpUsed = StatAllocator.totalCpUsed(stats, effMin)
                Button(
                    onClick = {
                        onRegister(username, password, characterName, selectedClassId, selectedRaceId, selectedGender, stats)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = authState !is AuthState.Loading &&
                            cpUsed == StatAllocator.CP_POOL &&
                            username.isNotBlank() && password.isNotBlank() && characterName.isNotBlank()
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("Create Character")
                    }
                }
            }
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

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    val stepLabels = listOf("Account", "Gender", "Race", "Class", "Stats", "Review")
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSteps) {
            val isActive = i == currentStep
            val isDone = i < currentStep
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isActive -> MaterialTheme.colorScheme.primary
                                isDone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${i + 1}",
                        color = if (isActive || isDone) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = stepLabels[i],
                    fontSize = 10.sp,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (i < totalSteps - 1) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(
                            if (i < currentStep) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
    }
}

@Composable
private fun CredentialsStep(
    username: String,
    password: String,
    characterName: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onCharacterNameChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Step 1: Account Details", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
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
            onValueChange = onCharacterNameChange,
            label = { Text("Character Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
    }
}

@Composable
private fun GenderSelectionStep(
    selectedGender: String,
    onGenderSelected: (String) -> Unit
) {
    val genders = listOf(
        Triple("male", "Male", "Your character presents as male."),
        Triple("female", "Female", "Your character presents as female."),
        Triple("neutral", "Neutral", "Your character presents as gender-neutral.")
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Step 2: Choose Gender", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        genders.forEach { (id, label, description) ->
            val isSelected = selectedGender == id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onGenderSelected(id) }
                    .then(
                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        else Modifier
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RaceSelectionStep(
    availableRaces: List<RaceDef>,
    selectedRaceId: String,
    onRaceSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Step 3: Choose Race", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (availableRaces.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
            Text("Loading races...", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(availableRaces) { race ->
                    val isSelected = selectedRaceId == race.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onRaceSelected(race.id) }
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                else Modifier
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(race.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                race.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Stat modifier chips
                            val m = race.statModifiers
                            val mods = listOf(
                                "STR" to m.strength, "AGI" to m.agility, "INT" to m.intellect,
                                "WIL" to m.willpower, "HLT" to m.health, "CHM" to m.charm
                            ).filter { it.second != 0 }
                            if (mods.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    mods.forEach { (name, value) ->
                                        val color = if (value > 0) Color(0xFF55FF55) else Color(0xFFFF5555)
                                        val text = "${name}:${if (value > 0) "+" else ""}$value"
                                        Text(
                                            text = text,
                                            fontSize = 11.sp,
                                            color = color,
                                            modifier = Modifier
                                                .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            if (race.xpModifier != 1.0) {
                                Text(
                                    "XP: ${(race.xpModifier * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF5555).copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassSelectionStep(
    availableClasses: List<CharacterClassDef>,
    selectedClassId: String,
    onClassSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Step 4: Choose Class", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (availableClasses.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
            Text("Loading classes...", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(availableClasses) { cls ->
                    val isSelected = selectedClassId == cls.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onClassSelected(cls.id) }
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                else Modifier
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(cls.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                cls.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Magic schools
                            if (cls.magicSchools.isNotEmpty()) {
                                val schools = cls.magicSchools.entries.joinToString(", ") { "${it.key} ${it.value}" }
                                Text(
                                    "Magic: $schools",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9B59FF)
                                )
                            }
                            // HP/MP per level
                            val hpText = "HP/lvl: ${cls.hpPerLevelMin}-${cls.hpPerLevelMax}"
                            val mpText = if (cls.mpPerLevelMax > 0) " | MP/lvl: ${cls.mpPerLevelMin}-${cls.mpPerLevelMax}" else ""
                            Text(
                                "$hpText$mpText",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            // Min stats
                            val s = cls.minimumStats
                            Text(
                                "Min: STR:${s.strength} AGI:${s.agility} INT:${s.intellect} WIL:${s.willpower} HLT:${s.health} CHM:${s.charm}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatAllocationStep(
    effectiveMinimum: Stats,
    allocatedStats: Stats,
    onStatsChanged: (Stats) -> Unit
) {
    val cpUsed = StatAllocator.totalCpUsed(allocatedStats, effectiveMinimum)
    val cpRemaining = StatAllocator.CP_POOL - cpUsed

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Step 5: Allocate Stats", style = MaterialTheme.typography.titleMedium)
            Text(
                "CP: $cpUsed / ${StatAllocator.CP_POOL}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (cpRemaining == 0) Color(0xFF55FF55) else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Header row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Stat", modifier = Modifier.width(80.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Text("Value", modifier = Modifier.width(40.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.width(72.dp)) // buttons space
            Text("Cost", modifier = Modifier.width(36.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(4.dp))

        val statEntries = listOf(
            StatEntry("Strength", allocatedStats.strength, effectiveMinimum.strength) { v -> allocatedStats.copy(strength = v) },
            StatEntry("Agility", allocatedStats.agility, effectiveMinimum.agility) { v -> allocatedStats.copy(agility = v) },
            StatEntry("Intellect", allocatedStats.intellect, effectiveMinimum.intellect) { v -> allocatedStats.copy(intellect = v) },
            StatEntry("Willpower", allocatedStats.willpower, effectiveMinimum.willpower) { v -> allocatedStats.copy(willpower = v) },
            StatEntry("Health", allocatedStats.health, effectiveMinimum.health) { v -> allocatedStats.copy(health = v) },
            StatEntry("Charm", allocatedStats.charm, effectiveMinimum.charm) { v -> allocatedStats.copy(charm = v) }
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(statEntries) { entry ->
                StatAllocationRow(
                    entry = entry,
                    cpRemaining = cpRemaining,
                    onStatsChanged = onStatsChanged
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { onStatsChanged(effectiveMinimum) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Reset Stats")
        }
    }
}

@Composable
private fun CharacterPreviewStep(
    characterName: String,
    selectedRace: RaceDef?,
    selectedClass: CharacterClassDef?,
    selectedGender: String,
    allocatedStats: Stats,
    serverBaseUrl: String
) {
    val raceName = selectedRace?.name ?: "Unknown"
    val className = selectedClass?.name ?: "Unknown"
    val genderLabel = selectedGender.replaceFirstChar { it.uppercase() }

    // Build sprite URL: images/players/{race}_{gender}_{class}.webp
    val raceId = (selectedRace?.id ?: "HUMAN").lowercase()
    val classId = (selectedClass?.id ?: "WARRIOR").lowercase()
    val spriteUrl = "$serverBaseUrl/assets/images/players/${raceId}_${selectedGender}_${classId}.webp"

    val dimText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val accentColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Step 6: Review Character", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Sprite + identity header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    AsyncImage(
                        model = coil3.request.ImageRequest.Builder(context)
                            .data(spriteUrl)
                            .crossfade(200)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = "$characterName sprite preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(120.dp)
                            .widthIn(max = 90.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = characterName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$genderLabel $raceName $className",
                            style = MaterialTheme.typography.bodyMedium,
                            color = dimText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // HP/MP per level
                        selectedClass?.let { cls ->
                            Text(
                                text = "HP/lvl: ${cls.hpPerLevelMin}-${cls.hpPerLevelMax}",
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50)
                            )
                            if (cls.mpPerLevelMax > 0) {
                                Text(
                                    text = "MP/lvl: ${cls.mpPerLevelMin}-${cls.mpPerLevelMax}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF448AFF)
                                )
                            }
                        }
                        // XP modifier
                        val combinedXpMod = (selectedRace?.xpModifier ?: 1.0) * (selectedClass?.xpModifier ?: 1.0)
                        if (combinedXpMod != 1.0) {
                            Text(
                                text = "XP rate: ${(combinedXpMod * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = if (combinedXpMod > 1.0) Color(0xFFFF5555) else Color(0xFF55FF55)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Stats
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Stats", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accentColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    val stats = listOf(
                        "STR" to allocatedStats.strength,
                        "AGI" to allocatedStats.agility,
                        "INT" to allocatedStats.intellect,
                        "WIL" to allocatedStats.willpower,
                        "HLT" to allocatedStats.health,
                        "CHM" to allocatedStats.charm
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        stats.forEach { (name, value) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = name, fontSize = 11.sp, color = dimText)
                                Text(text = "$value", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    // Race stat modifiers
                    selectedRace?.let { race ->
                        val m = race.statModifiers
                        val mods = listOf(
                            "STR" to m.strength, "AGI" to m.agility, "INT" to m.intellect,
                            "WIL" to m.willpower, "HLT" to m.health, "CHM" to m.charm
                        ).filter { it.second != 0 }
                        if (mods.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Race:", fontSize = 11.sp, color = dimText)
                                mods.forEach { (name, value) ->
                                    val color = if (value > 0) Color(0xFF55FF55) else Color(0xFFFF5555)
                                    Text(
                                        text = "${name}:${if (value > 0) "+" else ""}$value",
                                        fontSize = 11.sp,
                                        color = color,
                                        modifier = Modifier
                                            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Class details: description, skills, magic
        item {
            selectedClass?.let { cls ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Class: ${cls.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accentColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cls.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        // Magic schools
                        if (cls.magicSchools.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Magic Schools", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9B59FF))
                            cls.magicSchools.forEach { (school, level) ->
                                Text(
                                    text = "${school.replaceFirstChar { it.uppercase() }} (tier $level)",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9B59FF).copy(alpha = 0.8f),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        // Skills
                        if (cls.skills.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Skills", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                cls.skills.forEach { skill ->
                                    Text(
                                        text = skill.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
                                        fontSize = 11.sp,
                                        color = Color(0xFFFF9800),
                                        modifier = Modifier
                                            .background(Color(0xFFFF9800).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Race details
        item {
            selectedRace?.let { race ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Race: ${race.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accentColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = race.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Future AI generation placeholder
        item {
            OutlinedButton(
                onClick = { /* Future: trigger on-device AI sprite generation */ },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate Custom Sprite (Coming Soon)")
            }
        }
    }
}

private data class StatEntry(
    val name: String,
    val current: Int,
    val minimum: Int,
    val withValue: (Int) -> Stats
)

@Composable
private fun StatAllocationRow(
    entry: StatEntry,
    cpRemaining: Int,
    onStatsChanged: (Stats) -> Unit
) {
    val costToAdd = StatAllocator.cpCostForPoint(entry.current, entry.minimum)
    val canAdd = cpRemaining >= costToAdd
    val canRemove = entry.current > entry.minimum

    val costColor = when {
        costToAdd == 1 -> Color(0xFF55FF55)  // green
        costToAdd == 2 -> Color(0xFFFFFF55)  // yellow
        else -> Color(0xFFFF5555)            // red
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.name,
            modifier = Modifier.width(80.dp),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "${entry.current}",
            modifier = Modifier.width(40.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Minus button
        FilledIconButton(
            onClick = {
                if (canRemove) {
                    onStatsChanged(entry.withValue(entry.current - 1))
                }
            },
            enabled = canRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Text("-", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Plus button
        FilledIconButton(
            onClick = {
                if (canAdd) {
                    onStatsChanged(entry.withValue(entry.current + 1))
                }
            },
            enabled = canAdd,
            modifier = Modifier.size(32.dp)
        ) {
            Text("+", fontWeight = FontWeight.Bold)
        }

        // Cost indicator
        Text(
            text = "${costToAdd}cp",
            modifier = Modifier.width(36.dp),
            fontSize = 12.sp,
            color = costColor,
            textAlign = TextAlign.Center
        )
    }
}
