package com.focusguard.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.R
import com.focusguard.state.EarnedItStore
import com.focusguard.state.EarnedItUiState
import com.focusguard.state.FocusSessionSummary
import com.focusguard.state.PetProfile
import com.focusguard.state.PurchaseResult
import com.focusguard.ui.theme.EarnedColors
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

private val HomeTitleColor = Color(0xFF1F3D24)
private val HomeSubtitleColor = HomeTitleColor.copy(alpha = 0.65f)

@Composable
fun HomeScreen(onStartSession: () -> Unit, onReplayOnboarding: () -> Unit) {
    val uiState by EarnedItStore.state.collectAsState()
    val haptics = rememberHaptics()
    var showPetCollection by remember { mutableStateOf(false) }

    if (showPetCollection) {
        PetCollectionBottomSheet(
            currentPet = uiState.pet,
            unlockedPetSpecies = uiState.unlockedPetSpecies,
            totalPoints = uiState.points,
            onDismiss = { showPetCollection = false },
            onSelectPet = { species, stage ->
                haptics.confirm()
                EarnedItStore.pickPetVersion(species.id, species.displayName, stage)
                showPetCollection = false
            },
            onUnlockPet = { species, costMinutes ->
                haptics.confirm()
                EarnedItStore.unlockPetSpecies(species.id, costMinutes) == PurchaseResult.Success
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(EarnedColors.LightBg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item { Greeter(petName = uiState.pet.name, streakDays = uiState.streakDays) }
        item {
            PetHeroCard(
                pet = uiState.pet,
                onClick = {
                    haptics.select()
                    showPetCollection = true
                }
            )
        }
        item {
            StartFocusCta(onClick = {
                haptics.confirm()
                onStartSession()
            })
        }
        item { StatRibbon(state = uiState) }
        item { DailyGoalsSection(uiState.sessionsToday, uiState.focusMinutesToday) }
        item { FooterChip() }
        item {
            ReplayLink(onClick = {
                haptics.tap()
                onReplayOnboarding()
            })
        }
        item {
            DemoModeToggle(
                isDemoMode = uiState.settings.demoModeEnabled,
                onToggle = {
                    haptics.confirm()
                    EarnedItStore.setDemoMode(!uiState.settings.demoModeEnabled)
                }
            )
        }
    }
}

@Composable
private fun Greeter(petName: String, streakDays: Int) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            else -> "Good evening,"
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                greeting,
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                color = HomeSubtitleColor
            )
            Text(
                "$petName's looking good",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = HomeTitleColor,
                lineHeight = 30.sp
            )
        }
        if (streakDays > 0) {
            StreakChip(days = streakDays)
        }
    }
}

@Composable
private fun StreakChip(days: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = EarnedColors.Warning.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = EarnedColors.Warning,
                modifier = Modifier.size(16.dp)
            )
            Text(
                "$days-day streak",
                color = EarnedColors.Warning,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PetHeroCard(pet: PetProfile, onClick: () -> Unit) {
    val happy = pet.fullness > 60
    val washColor = if (happy) EarnedColors.Focus else EarnedColors.Warning
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = homePetSceneRes(pet)),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.50f to Color.Transparent,
                                0.78f to Color.White.copy(alpha = 0.82f),
                                1.00f to Color.White
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(washColor.copy(alpha = 0.08f), Color.Transparent)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Spacer(Modifier.height(180.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    pet.name,
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = HomeTitleColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${pet.mood} · fullness ${pet.fullness}%",
                    fontSize = 13.sp,
                    color = HomeSubtitleColor
                )
                Spacer(Modifier.height(12.dp))
                FullnessBar(fullness = pet.fullness, happy = happy)
            }
        }
    }
}

@DrawableRes
private fun homePetSceneRes(pet: PetProfile): Int {
    val species = pet.species.lowercase()
    val stage = when {
        pet.stage <= 1 -> "egg"
        pet.stage <= 3 -> "kid"
        else -> "adult"
    }

    return when (species to stage) {
        "lumi" to "egg" -> R.drawable.home_pet_lumi_egg
        "lumi" to "kid" -> R.drawable.home_pet_lumi_kid
        "lumi" to "adult" -> R.drawable.home_pet_lumi_adult
        "owly" to "egg" -> R.drawable.home_pet_owly_egg
        "owly" to "kid" -> R.drawable.home_pet_owly_kid
        "owly" to "adult" -> R.drawable.home_pet_owly_adult
        "kitsu" to "egg" -> R.drawable.home_pet_kitsu_egg
        "kitsu" to "kid" -> R.drawable.home_pet_kitsu_kid
        else -> R.drawable.home_pet_kitsu_adult
    }
}

@Composable
private fun FullnessBar(fullness: Int, happy: Boolean) {
    val barColor = if (happy) EarnedColors.Focus else EarnedColors.Warning
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(HomeTitleColor.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fullness.coerceIn(0, 100) / 100f)
                .height(6.dp)
                .background(barColor, RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun StartFocusCta(onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        label = "ctaScale"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        val released = tryAwaitRelease()
                        pressed = false
                        if (released) onClick()
                    }
                )
            },
        shape = RoundedCornerShape(22.dp),
        color = EarnedColors.Primary,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Start a focus",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Lock distractions · 25–60 min",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 13.sp
                )
            }
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.18f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRibbon(state: EarnedItUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Star,
                tint = EarnedColors.Points,
                label = "Points",
                value = "%,d".format(state.points)
            )
            StatChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.LocalFireDepartment,
                tint = EarnedColors.Warning,
                label = "Streak",
                value = "${state.streakDays} d"
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Spa,
                tint = EarnedColors.Focus,
                label = "Today's focus",
                value = "${state.focusMinutesToday}m"
            )
            StatChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Timer,
                tint = EarnedColors.Primary,
                label = "Time bank",
                value = "${state.timeBankMinutes}m"
            )
        }
    }
}

@Composable
private fun StatChip(
    modifier: Modifier,
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = HomeSubtitleColor,
                fontSize = 10.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = HomeTitleColor
            )
        }
    }
}

@Composable
private fun DailyGoalsSection(
    sessionsToday: List<FocusSessionSummary>,
    focusMinutesToday: Int,
) {
    val pointsToday = sessionsToday.sumOf { it.pointsEarned }
    val focusSessions = sessionsToday.count { it.success }
    val bestScore = sessionsToday.maxOfOrNull { it.focusScore } ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Daily challenges",
            fontFamily = FontFamily.Serif,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = HomeTitleColor
        )

        GoalCard(
            title = "Earn 100 pts from sessions",
            detail = "$pointsToday / 100 pts earned today",
            progress = pointsToday / 100f,
            icon = Icons.Filled.Star,
            tint = EarnedColors.Points,
            reward = "+100 pts"
        )
        GoalCard(
            title = "Complete 2 focus sessions",
            detail = "$focusSessions of 2 finished today",
            progress = focusSessions / 2f,
            icon = Icons.Filled.Spa,
            tint = EarnedColors.Focus,
            reward = "+100 pts"
        )
        GoalCard(
            title = "Score 80%+ in a session",
            detail = if (bestScore >= 80) "Best score ${bestScore}%" else "Best so far ${bestScore}%",
            progress = if (bestScore >= 80) 1f else bestScore / 80f,
            icon = Icons.Filled.Timer,
            tint = EarnedColors.Primary,
            reward = "+100 pts"
        )

        if (sessionsToday.isNotEmpty()) {
            TodayRecap(sessionsToday.first())
        }
    }
}

@Composable
private fun GoalCard(
    title: String,
    detail: String,
    progress: Float,
    icon: ImageVector,
    tint: Color,
    reward: String = "",
) {
    val done = progress >= 1f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(550),
        label = "dailyGoalProgress"
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = if (done) tint.copy(alpha = 0.22f) else tint.copy(alpha = 0.14f)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HomeTitleColor)
                    if (reward.isNotEmpty()) {
                        Text(
                            reward,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (done) EarnedColors.Focus else EarnedColors.Points,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(detail, fontSize = 12.sp, color = HomeSubtitleColor)
                Spacer(Modifier.height(9.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = tint,
                    trackColor = HomeTitleColor.copy(alpha = 0.08f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun TodayRecap(session: FocusSessionSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = HomeTitleColor.copy(alpha = 0.04f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Latest session", fontSize = 12.sp, color = HomeSubtitleColor)
                Text(
                    "${session.durationMinutes} min · ${session.focusScore}% focus",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = HomeTitleColor
                )
            }
            Text(
                formatSessionTime(session.startTimeMs),
                fontSize = 12.sp,
                color = HomeSubtitleColor
            )
        }
    }
}

private fun formatSessionTime(timestampMs: Long): String {
    val time = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault()).toLocalTime()
    val hour = time.hour % 12
    val displayHour = if (hour == 0) 12 else hour
    val suffix = if (time.hour < 12) "AM" else "PM"
    return "%d:%02d %s".format(displayHour, time.minute, suffix)
}

@Composable
private fun FooterChip() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = EarnedColors.Focus.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Filled.Eco,
                contentDescription = null,
                tint = EarnedColors.Focus,
                modifier = Modifier.size(16.dp)
            )
            Text(
                "100% on-device · Snapdragon NPU",
                fontSize = 12.sp,
                color = EarnedColors.Focus,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DemoModeToggle(isDemoMode: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .clickable(onClick = onToggle)
                .padding(4.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (isDemoMode) EarnedColors.Warning.copy(alpha = 0.12f) else EarnedColors.Primary.copy(alpha = 0.10f)
        ) {
            Text(
                if (isDemoMode) "Unload demo data" else "Load demo data",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDemoMode) EarnedColors.Warning else EarnedColors.Primary
            )
        }
    }
}

@Composable
private fun ReplayLink(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Replay onboarding",
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClick() })
                }
                .padding(8.dp),
            fontSize = 12.sp,
            color = HomeSubtitleColor,
            textDecoration = TextDecoration.Underline
        )
    }
}
