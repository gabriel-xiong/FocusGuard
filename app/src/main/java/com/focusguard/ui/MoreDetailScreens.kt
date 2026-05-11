package com.focusguard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.state.AppSettings
import com.focusguard.state.EarnedItStore
import com.focusguard.state.FocusSessionSummary
import com.focusguard.state.PrivacyEvent
import com.focusguard.state.PurchaseResult
import com.focusguard.state.ScheduledFocusBlock
import com.focusguard.state.StorePurchase
import com.focusguard.state.TimeBankTransaction
import com.focusguard.ui.theme.EarnedColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun MoreFeatureScreen(title: String, onBack: () -> Unit) {
    when (title.lowercase()) {
        "trophies" -> TrophiesScreen(onBack)
        "focus dna" -> FocusDnaScreen(onBack)
        "wrapped" -> WrappedScreen(onBack)
        "store" -> StoreScreen(onBack)
        "desk audit" -> DeskAuditScreen(onBack)
        "calendar" -> CalendarScreen(onBack)
        "time bank" -> TimeBankScreen(onBack)
        "privacy ledger" -> PrivacyLedgerScreen(onBack)
        "settings" -> SettingsScreen(onBack)
        else -> MoreFallbackScreen(title, onBack)
    }
}

@Composable
fun MorePetDetailScreen(onBack: () -> Unit) {
    val state by EarnedItStore.state.collectAsState()
    val stageLabel = petStageLabel(state.pet.stage)
    val nextStagePts = com.focusguard.state.pointsForNextStage(state.pet.stage)
    val evolutionProgress = if (nextStagePts == null) {
        1f
    } else {
        (state.points / nextStagePts.toFloat()).coerceIn(0f, 1f)
    }
    var moodBoost by remember { mutableStateOf("Ready") }

    MoreDetailScaffold(
        title = "Focus Pet",
        subtitle = "Care, growth, and unlock progress.",
        onBack = onBack
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PetSprite(state.pet, size = 190.dp)
                    Text(state.pet.name, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    StatusPill("${state.pet.mood} mood", EarnedColors.Focus)
                    if (state.pet.equippedCosmetic.isNotBlank()) {
                        StatusPill("${state.pet.equippedCosmetic} equipped", EarnedColors.Points)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        KpiTile("Stage", stageLabel, Modifier.weight(1f))
                        KpiTile("Fullness", "${state.pet.fullness}%", Modifier.weight(1f))
                        KpiTile("Streak", "${state.streakDays}d", Modifier.weight(1f))
                    }
                    ProgressBlock(
                        title = if (nextStagePts == null) "Max evolution" else "Next evolution",
                        detail = if (nextStagePts == null) {
                            "%,d total points earned.".format(state.points)
                        } else {
                            "%,d / %,d pts to evolve".format(state.points, nextStagePts)
                        },
                        progress = evolutionProgress,
                        color = EarnedColors.Points
                    )
                }
            }
        }
        item {
            SectionCard("Care actions", "Use points and focus progress to keep your pet energized.") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    CareAction("Feed", "25 pts", Icons.Filled.Redeem, Modifier.weight(1f)) {
                        moodBoost = if (EarnedItStore.feedPet()) "Full and focused" else "Not enough points yet"
                    }
                    CareAction("Play", "Free", Icons.Filled.Pets, Modifier.weight(1f)) {
                        EarnedItStore.playWithPet()
                        moodBoost = "Playful"
                    }
                    CareAction("Encourage", "Free", Icons.Filled.AutoAwesome, Modifier.weight(1f)) {
                        EarnedItStore.encouragePet()
                        moodBoost = "Brave"
                    }
                }
                Text(
                    moodBoost,
                    color = EarnedColors.Focus,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
        item {
            SectionCard("Milestones", "Recent growth moments") {
                StatusRow(Icons.Filled.CheckCircle, "$stageLabel form unlocked", "%,d total points earned.".format(state.points), EarnedColors.Focus)
                StatusRow(
                    Icons.Filled.WorkspacePremium,
                    if (nextStagePts == null) "Fully evolved" else "Next evolution",
                    if (nextStagePts == null) "Your pet has reached its final form." else "%,d pts remaining.".format((nextStagePts - state.points).coerceAtLeast(0)),
                    EarnedColors.Points
                )
                StatusRow(Icons.Filled.Lock, "Night aura", "Requires a 14 day streak. Current streak: ${state.streakDays}.", MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TrophiesScreen(onBack: () -> Unit) {
    val state by EarnedItStore.state.collectAsState()
    val cleanDeskProgress = (state.deskAudit.score / 85f).coerceIn(0f, 1f)
    val trophies = listOf(
        Trophy("First Lock", "Complete your first protected focus session.", if (state.allSessions.isNotEmpty()) 1f else 0f, state.allSessions.isNotEmpty()),
        Trophy("Seven Day Spark", "Hold a 7 day streak.", (state.streakDays / 7f).coerceIn(0f, 1f), state.streakDays >= 7),
        Trophy("Deep Work Bronze", "Earn 1,000 total points.", (state.points / 1000f).coerceIn(0f, 1f), state.points >= 1000),
        Trophy("Clean Desk", "Pass a desk audit with 85+.", cleanDeskProgress, state.deskAudit.score >= 85),
        Trophy("Reward Saver", "Bank 60 reward minutes.", (state.timeBankMinutes / 60f).coerceIn(0f, 1f), state.timeBankMinutes >= 60),
        Trophy("Pet Guardian", "Evolve your pet into its adult form.", (state.pet.stage / 3f).coerceIn(0f, 1f), state.pet.stage >= 3)
    )
    val earned = trophies.filter { it.earned }
    val nextTrophy = trophies.filterNot { it.earned }.maxByOrNull { it.progress }

    MoreDetailScaffold("Trophies", "Badges, milestones, and next unlocks.", onBack) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiTile("Earned", earned.size.toString(), Modifier.weight(1f))
                KpiTile("Locked", trophies.count { !it.earned }.toString(), Modifier.weight(1f))
                KpiTile("Next", "${((nextTrophy?.progress ?: 1f) * 100).toInt()}%", Modifier.weight(1f))
            }
        }
        item {
            SectionCard("Latest unlock", "Computed from your local progress") {
                if (earned.isNotEmpty()) {
                    val latest = earned.last()
                    StatusRow(Icons.Filled.EmojiEvents, latest.title, latest.detail, EarnedColors.Points)
                } else {
                    StatusRow(Icons.Filled.Lock, "No trophies yet", "Complete your first focus session to unlock one.", MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(trophies) { trophy ->
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IconBadge(
                        icon = if (trophy.earned) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                        color = if (trophy.earned) EarnedColors.Points else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(trophy.title, fontWeight = FontWeight.Bold)
                        Text(trophy.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        ProgressBar(trophy.progress, if (trophy.earned) EarnedColors.Points else EarnedColors.Primary)
                    }
                    Text("${(trophy.progress * 100).toInt()}%", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun FocusDnaScreen(onBack: () -> Unit) {
    val state by EarnedItStore.state.collectAsState()
    val sessions = state.allSessions
    val avgScore = state.averageFocusScore
    val consistency = (sessions.count { it.success } / sessions.size.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val recovery = if (sessions.isEmpty()) 0f else (1f - (sessions.map { it.recoverySeconds }.average().toFloat() / 180f)).coerceIn(0f, 1f)
    val risk = if (sessions.isEmpty()) 0f else (sessions.map { it.distractionCount }.average().toFloat() / 5f).coerceIn(0f, 1f)
    val archetype = when {
        sessions.size < 3 -> "Still Calibrating"
        avgScore >= 88 && consistency > 0.75f -> "Deep Work Builder"
        recovery > 0.75f -> "Fast Recoverer"
        state.weeklyFocusMinutes > 180 -> "Endurance Learner"
        else -> "Focus Sprinter"
    }
    val bestTime = sessions.groupBy { hourLabel(it.startTimeMs) }
        .maxByOrNull { entry -> entry.value.sumOf { it.durationMinutes } }
        ?.key ?: "After 3 sessions"

    MoreDetailScaffold("Focus DNA", "Your attention style and recovery profile.", onBack) {
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = EarnedColors.Primary.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(Icons.Filled.Fingerprint, EarnedColors.Primary)
                        Column {
                            Text(archetype, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (sessions.size < 3) "Complete ${3 - sessions.size} more sessions to unlock a sharper profile."
                                else "Best focus window: $bestTime. Average score: $avgScore%.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    ProgressBlock("Consistency", "${sessions.count { it.success }} of ${sessions.size} sessions completed successfully", consistency, EarnedColors.Focus)
                    ProgressBlock("Recovery", "Average bounce-back ${sessions.map { it.recoverySeconds }.averageOrZero().roundToInt()} seconds", recovery, EarnedColors.Secondary)
                    ProgressBlock("Distraction risk", "${sessions.sumOf { it.distractionCount }} distraction attempts logged", risk, EarnedColors.Warning)
                }
            }
        }
        item {
            SectionCard("Recommendations", "Three moves for your next session") {
                StatusRow(Icons.Filled.Schedule, "Protect $bestTime", "Your strongest sessions cluster around this time.", EarnedColors.Focus)
                StatusRow(Icons.Filled.Timer, if (risk > 0.45f) "Use shorter rounds" else "Keep current duration", if (risk > 0.45f) "Distraction risk is elevated; try 25 minutes." else "Your completion rate supports longer blocks.", EarnedColors.Secondary)
                StatusRow(Icons.Filled.Pets, "Reward after focus", "Feed ${state.pet.name} after session one to reinforce the loop.", EarnedColors.Points)
            }
        }
        item {
            SectionCard("Signal confidence", "Based on saved local focus history") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiTile("Sessions", sessions.size.toString(), Modifier.weight(1f))
                    KpiTile("Best time", bestTime, Modifier.weight(1f))
                    KpiTile("Avg score", "$avgScore%", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WrappedScreen(onBack: () -> Unit) {
    val state by EarnedItStore.state.collectAsState()
    val weekSessions = state.weekSessions
    val weeklyPoints = weekSessions.sumOf { it.pointsEarned }
    val best = state.bestSession
    val improvement = if (weekSessions.size >= 2) {
        weekSessions.first().focusScore - weekSessions.last().focusScore
    } else {
        0
    }

    MoreDetailScaffold("Wrapped", "A share-ready weekly focus recap.", onBack) {
        item {
            Surface(shape = RoundedCornerShape(30.dp), color = EarnedColors.Secondary.copy(alpha = 0.10f), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PetSprite(state.pet, size = 118.dp)
                    Text("${state.profile.displayName}'s focus week", fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("You turned ${state.weeklyFocusMinutes} focused minutes into ${state.timeBankMinutes} banked reward minutes.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        KpiTile("Minutes", state.weeklyFocusMinutes.toString(), Modifier.weight(1f))
                        KpiTile("Points", "+$weeklyPoints", Modifier.weight(1f))
                        KpiTile("Streak", "${state.streakDays}d", Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            SectionCard("Best moments", "Generated from this week's saved sessions") {
                StatusRow(Icons.Filled.CheckCircle, "Best session", best?.let { "${it.durationMinutes} minutes at ${it.focusScore}% focus." } ?: "Finish a session to create a highlight.", EarnedColors.Focus)
                StatusRow(Icons.Filled.Visibility, "Biggest improvement", if (improvement > 0) "+$improvement focus points across the week." else "Keep studying to unlock a comparison.", EarnedColors.Primary)
                StatusRow(Icons.Filled.Pets, "Pet growth", "${state.pet.name} stayed ${state.pet.mood.lowercase()} all week.", EarnedColors.Points)
            }
        }
    }
}

@Composable
private fun StoreScreen(onBack: () -> Unit) {
    val state by EarnedItStore.state.collectAsState()
    val categories = listOf("Rewards", "Pet")
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var lastMessage by remember { mutableStateOf("Purchases are saved locally on this device.") }
    val haptics = rememberHaptics()
    val items = listOf(
        StoreItem("youtube_15", "15 min YouTube pass", "Rewards", 500, "Unlock reward app time", 1),
        StoreItem("tiktok_30", "30 min TikTok pass", "Rewards", 1000, "Spend your earned points", 1),
        StoreItem("lumi_scarf", "Lumi scarf", "Pet", 300, "Equip a cozy pet cosmetic", 1),
        StoreItem("kitsu_bandana", "Kitsu bandana", "Pet", 400, "Equip a bright focus bandana", 1),
        StoreItem("owly_glasses", "Owly glasses", "Pet", 500, "Equip study glasses", 2),
        StoreItem("focus_crown", "Focus crown", "Pet", 750, "Evolved form required", 3)
    )

    MoreDetailScaffold("Store", "Spend points on rewards and pet upgrades.", onBack) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiTile("Points", "%,d".format(state.points), Modifier.weight(1f))
                KpiTile("Bank", "${state.timeBankMinutes}m", Modifier.weight(1f))
                KpiTile("Owned", state.storePurchases.size.toString(), Modifier.weight(1f))
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    FilterPill(category, selectedCategory == category) { selectedCategory = category }
                }
            }
        }
        item {
            Text(lastMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        items(items.filter { it.category == selectedCategory }) { item ->
            val isOwned = state.storePurchases.any { it.itemId == item.id }
            val isEquipped = item.category == "Pet" && state.pet.equippedCosmetic == item.name
            val locked = state.pet.stage < item.requiredStage
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IconBadge(
                        icon = if (item.category == "Rewards") Icons.Filled.Timer else if (item.category == "Pet") Icons.Filled.Pets else Icons.Filled.Store,
                        color = if (locked) MaterialTheme.colorScheme.onSurfaceVariant else EarnedColors.Primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, fontWeight = FontWeight.Bold)
                        Text(item.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Text("${item.price} pts", color = EarnedColors.Points, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            haptics.confirm()
                            when {
                                locked -> lastMessage = "Focus crown unlocks when your pet reaches adult form."
                                isOwned && item.category == "Pet" -> {
                                    EarnedItStore.equipPetCosmetic(item.name)
                                    lastMessage = "${item.name} equipped."
                                }
                                isOwned -> lastMessage = "${item.name} is already in your inventory."
                                else -> {
                                    lastMessage = when (EarnedItStore.purchaseStoreItem(item.id, item.name, item.category, item.price)) {
                                        PurchaseResult.Success -> "${item.name} added to inventory."
                                        PurchaseResult.AlreadyOwned -> "${item.name} is already in your inventory."
                                        PurchaseResult.InsufficientPoints -> "Not enough points yet. Finish one more session."
                                        PurchaseResult.Locked -> "${item.name} is locked."
                                    }
                                }
                            }
                        },
                        enabled = !locked && !isEquipped && !(isOwned && item.category != "Pet"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isOwned) EarnedColors.Focus else EarnedColors.Primary)
                    ) {
                        Text(
                            when {
                                locked -> "Locked"
                                isEquipped -> "Equipped"
                                isOwned && item.category == "Pet" -> "Equip"
                                isOwned -> "Owned"
                                else -> "Buy"
                            },
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeskAuditScreen(onBack: () -> Unit) {
    val state by EarnedItStore.state.collectAsState()
    val audit = state.deskAudit
    val resultVisible = audit.timestampMs > 0L
    val haptics = rememberHaptics()

    MoreDetailScaffold("Desk audit", "Score your workspace before focus starts.", onBack) {
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(if (resultVisible) Icons.Filled.CheckCircle else Icons.Filled.PhotoCamera, if (resultVisible) EarnedColors.Focus else EarnedColors.Primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (resultVisible) "Workspace score: ${audit.score}" else "Ready to scan", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("Camera stays local. Demo analysis can run without saving photos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = {
                            haptics.confirm()
                            EarnedItStore.runDeskAudit()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EarnedColors.Primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (resultVisible) Icons.Filled.AutoAwesome else Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (resultVisible) "Run again" else "Start desk audit")
                    }
                }
            }
        }
        if (resultVisible) {
            item {
                SectionCard("Results", "Small fixes before you start") {
                    AuditResult(Icons.Filled.WbSunny, "Lighting", if (audit.lightingScore >= 80) "Good" else "Improve", "Face and workspace lighting score: ${audit.lightingScore}.", EarnedColors.Focus, audit.lightingScore / 100f)
                    AuditResult(Icons.Filled.CleaningServices, "Clutter", if (audit.clutterScore >= 75) "Good" else "Improve", "Move extra devices out of reach if this dips.", EarnedColors.Warning, audit.clutterScore / 100f)
                    AuditResult(Icons.Filled.Visibility, "Phone visibility", if (audit.phoneRiskScore >= 60) "Risk" else "Clear", "Reward device risk score: ${audit.phoneRiskScore}.", EarnedColors.Danger, audit.phoneRiskScore / 100f)
                    AuditResult(Icons.Filled.AccessibilityNew, "Posture", if (audit.postureScore >= 80) "Good" else "Improve", "Camera angle score: ${audit.postureScore}.", EarnedColors.Focus, audit.postureScore / 100f)
                }
            }
            item {
                SectionCard("Saved summary", "Used to personalize the next session") {
                    StatusRow(Icons.Filled.CheckCircle, "Audit saved", "Workspace score will appear in your next session setup.", EarnedColors.Focus)
                }
            }
        } else {
            item {
                SectionCard("What it checks", "Useful even before live camera analysis") {
                    StatusRow(Icons.Filled.LightMode, "Lighting", "Can the app see you consistently?", EarnedColors.Points)
                    StatusRow(Icons.Filled.CleaningServices, "Clutter", "Are distractions inside arm's reach?", EarnedColors.Secondary)
                    StatusRow(Icons.Filled.Security, "Privacy", "No cloud upload is needed for the demo flow.", EarnedColors.Focus)
                }
            }
        }
    }
}

@Composable
private fun CalendarScreen(onBack: () -> Unit) {
    val state by EarnedItStore.state.collectAsState()
    var selectedDuration by remember { mutableIntStateOf(25) }
    var created by remember { mutableStateOf(false) }
    val blocks = state.scheduledBlocks.sortedBy { it.startTimeMs }
    val haptics = rememberHaptics()

    MoreDetailScaffold("Calendar", "Plan protected focus blocks.", onBack) {
        item {
            SectionCard("Create block", "Quick schedule for the next session") {
                Text("Focus block", fontWeight = FontWeight.Bold)
                Text("Today at 4:30 PM", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 50).forEach { minutes ->
                        FilterPill("${minutes}m", selectedDuration == minutes) { selectedDuration = minutes }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        haptics.confirm()
                        EarnedItStore.scheduleFocusBlock("Focus block", selectedDuration)
                        created = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EarnedColors.Primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Schedule ${selectedDuration}m session")
                }
                if (created) {
                    Text("Session added with reminders and the default blocked app set.", color = EarnedColors.Focus, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        item {
            SectionCard("This week", "Upcoming protected sessions") {
                if (blocks.isEmpty()) {
                    StatusRow(Icons.Filled.CalendarMonth, "No blocks scheduled", "Create one above to preload your next focus session.", MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    blocks.forEachIndexed { index, block ->
                        StatusRow(Icons.Filled.CalendarMonth, block.title, "${formatTimestamp(block.startTimeMs)} · ${block.durationMinutes}m · ${block.blockedApps.joinToString()}", if (index == 0) EarnedColors.Primary else EarnedColors.Secondary)
                        if (index != blocks.lastIndex) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    }
                }
            }
        }
        item {
            SectionCard("Schedule status", "Saved inside EarnedIt") {
                StatusRow(Icons.Filled.Security, "Conflict check", "No reward window overlaps detected.", EarnedColors.Focus)
            }
        }
    }
}

@Composable
private fun TimeBankScreen(onBack: () -> Unit) {
    val state by EarnedItStore.state.collectAsState()
    var redeemAmount by remember { mutableIntStateOf(15) }
    var message by remember { mutableStateOf("Earned minutes can be redeemed for reward apps after focus.") }
    val haptics = rememberHaptics()
    val earnedToday = state.timeBankTransactions
        .filter { it.minutes > 0 && isToday(it.timestampMs) }
        .sumOf { it.minutes }
    val lifetimeEarned = state.timeBankTransactions.filter { it.minutes > 0 }.sumOf { it.minutes }

    MoreDetailScaffold("Time bank", "Earned minutes for reward apps.", onBack) {
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = EarnedColors.Focus.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Available balance", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${state.timeBankMinutes} min", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = EarnedColors.Focus)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiTile("Today", "+${earnedToday}m", Modifier.weight(1f))
                        KpiTile("Lifetime", "${lifetimeEarned / 60}h ${lifetimeEarned % 60}m", Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            SectionCard("Redeem", "Choose an amount for reward apps") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 15, 30).forEach { minutes ->
                        FilterPill("${minutes}m", redeemAmount == minutes) { redeemAmount = minutes }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        haptics.confirm()
                        message = if (EarnedItStore.redeemTimeBank(redeemAmount, "YouTube")) {
                            "$redeemAmount minutes reserved for YouTube reward time."
                        } else {
                            "Not enough banked time. Complete one more focus session."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EarnedColors.Primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Redeem, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Redeem reward time")
                }
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            SectionCard("Ledger", "Recent earn and spend activity") {
                if (state.timeBankTransactions.isEmpty()) {
                    StatusRow(Icons.Filled.Timer, "No transactions yet", "Complete a successful focus session to earn reward minutes.", MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.timeBankTransactions.take(6).forEach { transaction ->
                        StatusRow(
                            if (transaction.minutes >= 0) Icons.Filled.CheckCircle else Icons.Filled.Timer,
                            transaction.title,
                            "${transaction.detail} · ${relativeTime(transaction.timestampMs)}",
                            if (transaction.minutes >= 0) EarnedColors.Focus else EarnedColors.Secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyLedgerScreen(onBack: () -> Unit) {
    val state by EarnedItStore.state.collectAsState()
    val permissions = state.permissions
    val haptics = rememberHaptics()

    MoreDetailScaffold("Privacy ledger", "What EarnedIt sees, stores, and never sends.", onBack) {
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = EarnedColors.Focus.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IconBadge(Icons.Filled.Shield, EarnedColors.Focus)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("On-device by design", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Camera, app-blocking, and focus scoring stay local in this demo build.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            SectionCard("Permission status", "Clear status before a judge tests the flow") {
                StatusRow(Icons.Filled.AccessibilityNew, "Accessibility", permissionCopy(permissions.accessibilityEnabled, "Used to show the block screen over distracting apps."), permissionColor(permissions.accessibilityEnabled))
                StatusRow(Icons.Filled.CameraAlt, "Camera", permissionCopy(permissions.cameraGranted, "Used for presence and posture signals during focus."), permissionColor(permissions.cameraGranted))
                StatusRow(Icons.Filled.Notifications, "Notifications", permissionCopy(permissions.notificationsGranted, "Used for scheduled focus reminders."), permissionColor(permissions.notificationsGranted))
                StatusRow(Icons.Filled.Security, "Usage access", permissionCopy(permissions.usageAccessGranted, "Used only to identify selected blocked/reward apps."), permissionColor(permissions.usageAccessGranted))
            }
        }
        item {
            SectionCard("Recent privacy actions", "Local activity log") {
                if (state.privacyEvents.isEmpty()) {
                    StatusRow(Icons.Filled.Visibility, "No privacy events yet", "Local activity will appear here after sessions or settings changes.", MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.privacyEvents.take(8).forEach { event ->
                        StatusRow(privacyIcon(event), event.title, "${event.detail} · ${relativeTime(event.timestampMs)} · no upload", privacyColor(event))
                    }
                }
            }
        }
        item {
            SectionCard("Data controls", "Demo-safe controls") {
                OutlinedButton(onClick = {
                    haptics.confirm()
                    EarnedItStore.clearSessionHistory()
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Clear local session history")
                }
                TextButton(onClick = {
                    haptics.confirm()
                    EarnedItStore.resetDemoData()
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset demo data")
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    val state by EarnedItStore.state.collectAsState()
    val settings = state.settings
    val haptics = rememberHaptics()

    MoreDetailScaffold("Settings", "Profile, permissions, and demo controls.", onBack) {
        item {
            SectionCard("Profile", "Current demo account") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = CircleShape, color = EarnedColors.Primary.copy(alpha = 0.14f), modifier = Modifier.size(52.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(state.profile.displayName.take(1).uppercase(), color = EarnedColors.Primary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        }
                    }
                    Column {
                            Text(state.profile.displayName, fontWeight = FontWeight.Bold)
                            Text("@${state.profile.username} · Local profile", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            SectionCard("Preferences", "Controls that are safe to flip during demo") {
                SettingSwitch("Notifications", "Focus reminders and reward alerts", settings.notificationsEnabled) {
                    EarnedItStore.updateSettings(settings.copy(notificationsEnabled = it))
                }
                SettingSwitch("Haptics", "Gentle feedback when a block starts", settings.hapticsEnabled) {
                    EarnedItStore.updateSettings(settings.copy(hapticsEnabled = it))
                }
                SettingSwitch("Strict blocking", "End reward windows immediately on focus start", settings.strictBlockingEnabled) {
                    EarnedItStore.updateSettings(settings.copy(strictBlockingEnabled = it))
                }
            }
        }
        item {
            SectionCard("Demo controls", "Fast recovery for judging") {
                SettingSwitch("Demo mode", "Seed lots of points, sessions, pet progress, and reward time", settings.demoModeEnabled) {
                    EarnedItStore.setDemoMode(it)
                }
                Surface(onClick = {
                    haptics.confirm()
                    EarnedItStore.resetDemoData()
                }, color = Color.Transparent) {
                    StatusRow(Icons.Filled.RestartAlt, "Reset demo state", "Restores sample sessions, points, and Time Bank.", EarnedColors.Primary)
                }
            }
        }
        item {
            SectionCard("Permissions", "Quick links and status") {
                StatusRow(Icons.Filled.AccessibilityNew, "Accessibility service", permissionCopy(state.permissions.accessibilityEnabled, "Needed for app blocking flow."), permissionColor(state.permissions.accessibilityEnabled))
                StatusRow(Icons.Filled.CameraAlt, "Camera", permissionCopy(state.permissions.cameraGranted, "Ask when starting a scored session."), permissionColor(state.permissions.cameraGranted))
                StatusRow(Icons.Filled.Shield, "Privacy ledger", "Review local data controls any time.", EarnedColors.Focus)
            }
        }
    }
}

@Composable
private fun MoreFallbackScreen(title: String, onBack: () -> Unit) {
    MoreDetailScaffold(title, "This tool is not available in this build.", onBack) {
        item {
            SectionCard("Unavailable", "Removed from the main More tab until it has a real flow.") {
                StatusRow(Icons.Filled.Lock, title, "No demo-only placeholder is shown in the app navigation.", EarnedColors.Primary)
            }
        }
    }
}

@Composable
private fun MoreDetailScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    val haptics = rememberHaptics()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = {
                        haptics.tap()
                        onBack()
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
        content()
    }
}

@Composable
private fun SectionCard(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            content()
        }
    }
}

@Composable
private fun KpiTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector, color: Color) {
    Surface(shape = RoundedCornerShape(14.dp), color = color.copy(alpha = 0.13f), modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(25.dp))
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val haptics = rememberHaptics()

    Surface(
        modifier = Modifier.clickable {
            haptics.select()
            onClick()
        },
        shape = RoundedCornerShape(999.dp),
        color = if (selected) EarnedColors.Primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) EarnedColors.Primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun StatusRow(icon: ImageVector, title: String, body: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = CircleShape, color = color.copy(alpha = 0.12f), modifier = Modifier.size(38.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProgressBlock(title: String, detail: String, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text("${(progress.coerceIn(0f, 1f) * 100).toInt()}%", color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        ProgressBar(progress, color)
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun CareAction(label: String, cost: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptics = rememberHaptics()

    Surface(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = EarnedColors.Primary.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = EarnedColors.Primary)
            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(cost, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@Composable
private fun AuditResult(icon: ImageVector, label: String, state: String, detail: String, color: Color, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusRow(icon, "$label · $state", detail, color)
        ProgressBar(progress, color)
    }
}

@Composable
private fun SettingSwitch(title: String, body: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val haptics = rememberHaptics()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                haptics.select()
                onCheckedChange(it)
            }
        )
    }
}

@Composable
private fun ProgressBar(progress: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .background(color, RoundedCornerShape(8.dp))
        )
    }
}

private fun List<Int>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

private fun petStageLabel(stage: Int): String = when {
    stage <= 1 -> "Hatchling"
    stage == 2 -> "Sprout"
    stage == 3 -> "Scout"
    stage == 4 -> "Guardian"
    else -> "Champion"
}


private fun hourLabel(timestampMs: Long): String {
    val hour = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault()).hour
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val suffix = if (hour < 12) "AM" else "PM"
    return "$hour12 $suffix"
}

private fun formatTimestamp(timestampMs: Long): String {
    val formatter = DateTimeFormatter.ofPattern("EEE, h:mm a")
    return Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault()).format(formatter)
}

private fun isToday(timestampMs: Long): Boolean {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(timestampMs).atZone(zone).toLocalDate()
    return date == java.time.LocalDate.now(zone)
}

private fun relativeTime(timestampMs: Long): String {
    val diffMinutes = ((System.currentTimeMillis() - timestampMs) / 60_000L).coerceAtLeast(0)
    return when {
        diffMinutes < 1 -> "just now"
        diffMinutes < 60 -> "${diffMinutes}m ago"
        diffMinutes < 1_440 -> "${diffMinutes / 60}h ago"
        else -> "${diffMinutes / 1_440}d ago"
    }
}

private fun permissionCopy(enabled: Boolean, purpose: String): String {
    return if (enabled) "Enabled · $purpose" else "Not enabled yet · $purpose"
}

@Composable
private fun permissionColor(enabled: Boolean): Color {
    return if (enabled) EarnedColors.Focus else EarnedColors.Warning
}

private fun privacyIcon(event: PrivacyEvent): ImageVector {
    return when (event.type) {
        "session" -> Icons.Filled.Visibility
        "reward" -> Icons.Filled.Timer
        "audit" -> Icons.Filled.CameraAlt
        "settings" -> Icons.Filled.Settings
        "calendar" -> Icons.Filled.CalendarMonth
        "store" -> Icons.Filled.Store
        "pet" -> Icons.Filled.Pets
        else -> Icons.Filled.Shield
    }
}

@Composable
private fun privacyColor(event: PrivacyEvent): Color {
    return when (event.type) {
        "session" -> EarnedColors.Secondary
        "reward" -> EarnedColors.Focus
        "audit" -> EarnedColors.Primary
        "settings" -> EarnedColors.Points
        "store" -> EarnedColors.Primary
        "pet" -> EarnedColors.Focus
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private data class Trophy(
    val title: String,
    val detail: String,
    val progress: Float,
    val earned: Boolean
)

private data class StoreItem(
    val id: String,
    val name: String,
    val category: String,
    val price: Int,
    val detail: String,
    val requiredStage: Int
)
