package com.focusguard.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.R
import com.focusguard.session.SessionManager
import com.focusguard.session.SessionPhase
import com.focusguard.state.EarnedItStore
import com.focusguard.state.PetProfile
import com.focusguard.ui.theme.EarnedColors
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class FocusPhase {
    Focused,
    Refocus,
    Distracted
}

private data class FocusBackground(
    val id: String,
    val title: String,
    val costMinutes: Int,
    @DrawableRes val imageRes: Int,
)

private val focusBackgrounds = listOf(
    FocusBackground("cozy_desk", "Cozy Desk", 0, R.drawable.focus_bg_cozy_desk),
    FocusBackground("balcony_night", "Balcony Night", 20, R.drawable.focus_bg_balcony_night),
)

@Composable
fun SessionScreen(onSessionEnd: (endedEarly: Boolean) -> Unit) {
    val session by SessionManager.stateFlow.collectAsState()
    val isCalibrating by SessionManager.isCalibrating.collectAsState()
    val appState by EarnedItStore.state.collectAsState()

    LaunchedEffect(session.isActive, session.remainingSeconds) {
        if (!session.isActive && session.remainingSeconds <= 0) {
            onSessionEnd(false)
        }
    }

    val mlScore = (session.attentionScore * 100f).coerceIn(0f, 100f)
    val mlPhase = when {
        mlScore >= 70f -> FocusPhase.Focused
        mlScore >= 40f -> FocusPhase.Refocus
        else -> FocusPhase.Distracted
    }
    val phase = mlPhase
    val animatedDisplayScore by animateFloatAsState(
        targetValue = mlScore,
        animationSpec = tween(950, easing = EaseInOutCubic),
        label = "display_score"
    )
    val displayScore = animatedDisplayScore.roundToInt().coerceIn(0, 100)
    val phaseColor by animateColorAsState(
        targetValue = when (phase) {
            FocusPhase.Focused -> EarnedColors.Focus
            FocusPhase.Refocus -> EarnedColors.Warning
            FocusPhase.Distracted -> Color(0xFFE38A74)
        },
        animationSpec = tween(650),
        label = "phase_color"
    )
    val sceneDim by animateFloatAsState(
        targetValue = when (phase) {
            FocusPhase.Focused -> 0.04f
            FocusPhase.Refocus -> 0.12f
            FocusPhase.Distracted -> 0.22f
        },
        animationSpec = tween(800, easing = EaseInOutCubic),
        label = "scene_dim"
    )
    val amberGlow by animateFloatAsState(
        targetValue = if (phase == FocusPhase.Refocus) 0.42f else 0.12f,
        animationSpec = tween(700, easing = EaseInOutCubic),
        label = "amber_glow"
    )
    val redGlow by animateFloatAsState(
        targetValue = if (phase == FocusPhase.Distracted) 0.08f else 0f,
        animationSpec = tween(700, easing = EaseInOutCubic),
        label = "red_glow"
    )

    val minutes = session.remainingSeconds / 60
    val seconds = session.remainingSeconds % 60
    val totalDuration = session.initialDurationSeconds.coerceAtLeast(session.remainingSeconds)
    val progress = if (totalDuration > 0) 1f - (session.remainingSeconds.toFloat() / totalDuration) else 0f
    val selectedBackground = focusBackgrounds.firstOrNull { it.id == appState.selectedFocusBackground } ?: focusBackgrounds.first()

    var showEndDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SessionTopBar(blockedCount = session.blacklistedApps.size)

            if (isCalibrating && session.isActive) {
                Text(
                    text = "Calibrating camera...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }

            FocusCompanionHero(
                pet = appState.pet,
                background = selectedBackground,
                phase = phase,
                phaseColor = phaseColor,
                sceneDim = sceneDim,
                amberGlow = amberGlow,
                redGlow = redGlow,
                timer = "%02d:%02d".format(minutes, seconds),
                score = displayScore,
                progress = progress,
            )

            SessionControls(
                isPaused = session.phase == SessionPhase.Paused,
                onTogglePause = {
                    if (session.phase == SessionPhase.Paused) {
                        SessionManager.resumeSession()
                    } else {
                        SessionManager.pauseSession()
                    }
                },
                onEndSession = { showEndDialog = true },
            )
        }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("End session early?") },
            text = { Text("You'll forfeit any points for this session.") },
            confirmButton = {
                TextButton(onClick = {
                    showEndDialog = false
                    SessionManager.stopSession()
                    onSessionEnd(true)
                }) {
                    Text("End session", color = EarnedColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text("Keep going")
                }
            }
        )
    }
}

@Composable
private fun SessionTopBar(blockedCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = EarnedColors.Primary, modifier = Modifier.size(16.dp))
                Text("Session · $blockedCount blocked", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        ) {
            IconButton(onClick = { }) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun FocusCompanionHero(
    pet: PetProfile,
    background: FocusBackground,
    phase: FocusPhase,
    phaseColor: Color,
    sceneDim: Float,
    amberGlow: Float,
    redGlow: Float,
    timer: String,
    score: Int,
    progress: Float,
) {
    val infinite = rememberInfiniteTransition(label = "companion_motion")
    val lampPulse by infinite.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lamp_pulse"
    )
    val sceneDrift by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scene_drift"
    )
    val particleTime by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_time"
    )
    val sceneScale by animateFloatAsState(
        targetValue = when (phase) {
            FocusPhase.Focused -> 1.015f
            FocusPhase.Refocus -> 1.028f
            FocusPhase.Distracted -> 1.026f
        },
        animationSpec = tween(1100, easing = EaseInOutCubic),
        label = "scene_scale"
    )
    val sceneBlur by animateDpAsState(
        targetValue = if (phase == FocusPhase.Distracted) 0.22.dp else 0.dp,
        animationSpec = tween(850, easing = EaseInOutCubic),
        label = "scene_blur"
    )
    val ringScale by animateFloatAsState(
        targetValue = if (phase == FocusPhase.Distracted) 1.012f else 1f,
        animationSpec = tween(700, easing = EaseInOutCubic),
        label = "ring_scale"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(545.dp),
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = heroSceneRes(pet.species, pet.stage, background.id, phase),
                animationSpec = tween(850, easing = EaseInOutCubic),
                label = "focus_scene_phase"
            ) { sceneRes ->
                Image(
                    painter = painterResource(sceneRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = sceneScale
                            scaleY = sceneScale
                            translationX = sceneDrift * 8f
                            translationY = if (phase == FocusPhase.Distracted) sceneDrift * 5f else 0f
                        }
                        .blur(sceneBlur),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = sceneDim))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                EarnedColors.Warning.copy(alpha = lampPulse + amberGlow),
                                Color.Transparent
                            ),
                            center = Offset(95f, 355f),
                            radius = 420f
                        )
                    )
            )
            if (redGlow > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.Transparent, Color(0xFFE38A74).copy(alpha = redGlow)),
                                radius = 760f
                            )
                        )
                )
            }
            AmbientParticles(
                phase = phase,
                phaseColor = phaseColor,
                time = particleTime,
                modifier = Modifier.fillMaxSize()
            )

            TimerRing(
                progress = progress,
                color = phaseColor,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .size(245.dp)
                    .scale(ringScale)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 86.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    timer,
                    color = Color.White,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                )
                Surface(shape = RoundedCornerShape(18.dp), color = phaseColor) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Spa, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Text("${phaseLabel(phase)} · $score%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text("ML tracking active · on-device", color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp)
                if (phase != FocusPhase.Focused) {
                    Spacer(Modifier.height(9.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Black.copy(alpha = 0.42f),
                        border = BorderStroke(1.dp, phaseColor.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = phaseMessage(phase),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            PhaseStrip(
                phase = phase,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 13.dp, vertical = 18.dp)
            )
        }
    }
}

@Composable
private fun TimerRing(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 5.dp.toPx()
        val padding = stroke / 2
        drawArc(
            color = Color.White.copy(alpha = 0.22f),
            startAngle = -210f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(padding, padding),
            size = Size(size.width - padding * 2, size.height - padding * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = -210f,
            sweepAngle = 300f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(padding, padding),
            size = Size(size.width - padding * 2, size.height - padding * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun AmbientParticles(
    phase: FocusPhase,
    phaseColor: Color,
    time: Float,
    modifier: Modifier = Modifier,
) {
    val particles = remember {
        listOf(
            Offset(0.18f, 0.22f),
            Offset(0.32f, 0.18f),
            Offset(0.72f, 0.20f),
            Offset(0.86f, 0.34f),
            Offset(0.22f, 0.58f),
            Offset(0.68f, 0.52f),
            Offset(0.42f, 0.70f),
            Offset(0.82f, 0.76f)
        )
    }
    Canvas(modifier = modifier) {
        particles.forEachIndexed { index, base ->
            val shimmer = ((sin((time * 6.283f) + index * 0.9f) + 1f) / 2f)
            val driftY = (time * 18f + index * 2.5f) % 18f
            val radius = when (phase) {
                FocusPhase.Focused -> 1.3f + shimmer * 1.1f
                FocusPhase.Refocus -> 1.7f + shimmer * 1.6f
                FocusPhase.Distracted -> 1.1f + shimmer * 0.8f
            }
            val alpha = when (phase) {
                FocusPhase.Focused -> 0.18f + shimmer * 0.22f
                FocusPhase.Refocus -> 0.24f + shimmer * 0.30f
                FocusPhase.Distracted -> 0.10f + shimmer * 0.12f
            }
            drawCircle(
                color = phaseColor.copy(alpha = alpha),
                radius = radius.dp.toPx(),
                center = Offset(
                    x = base.x * size.width,
                    y = base.y * size.height - driftY
                )
            )
        }
    }
}

@Composable
private fun PhaseStrip(phase: FocusPhase, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PhaseHint(
                icon = Icons.Filled.Spa,
                title = "Focused",
                subtitle = "You're in the zone!",
                color = EarnedColors.Focus,
                active = phase == FocusPhase.Focused,
                modifier = Modifier.weight(1f)
            )
            PhaseHint(
                icon = Icons.Filled.Headphones,
                title = "Refocus",
                subtitle = "Stay with it.",
                color = EarnedColors.Warning,
                active = phase == FocusPhase.Refocus,
                modifier = Modifier.weight(1f)
            )
            PhaseHint(
                icon = Icons.Filled.Close,
                title = "Distracted",
                subtitle = "Let's get back.",
                color = Color(0xFFE38A74),
                active = phase == FocusPhase.Distracted,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PhaseHint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.alpha(if (active) 1f else 0.72f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Column {
            Text(title, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.78f), fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SessionControls(
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onEndSession: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isPaused) EarnedColors.Focus.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (isPaused) EarnedColors.Focus else MaterialTheme.colorScheme.outlineVariant)
        ) {
            IconButton(onClick = onTogglePause) {
                Icon(
                    if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    tint = if (isPaused) EarnedColors.Focus else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Button(
            onClick = onEndSession,
            modifier = Modifier
                .weight(1f)
                .height(58.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EarnedColors.Primary, contentColor = Color.White)
        ) {
            Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (isPaused) "Paused" else "End Session",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@DrawableRes
private fun heroSceneRes(species: String, stage: Int, backgroundId: String, phase: FocusPhase): Int {
    val focusStage = focusSceneStage(species, stage)
    return when (species) {
        "owly" -> if (backgroundId == "balcony_night") {
            R.drawable.focus_scene_owly_balcony_night
        } else {
            when (focusStage) {
                1 -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_owly_1_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_owly_1_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_owly_1_cozy_desk_distracted
                }
                5 -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_owly_5_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_owly_5_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_owly_5_cozy_desk_distracted
                }
                else -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_owly_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_owly_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_owly_cozy_desk_distracted
                }
            }
        }
        "lumi" -> if (backgroundId == "balcony_night") {
            R.drawable.focus_scene_lumi_balcony_night
        } else {
            when (focusStage) {
                1 -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_lumi_1_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_lumi_1_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_lumi_1_cozy_desk_distracted
                }
                4 -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_lumi_4_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_lumi_4_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_lumi_4_cozy_desk_distracted
                }
                else -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_lumi_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_lumi_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_lumi_cozy_desk_distracted
                }
            }
        }
        else -> if (backgroundId == "balcony_night") {
            R.drawable.focus_scene_kitsu_balcony_night
        } else {
            when (focusStage) {
                1 -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_kitsu_1_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_kitsu_1_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_kitsu_1_cozy_desk_distracted
                }
                5 -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_kitsu_5_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_kitsu_5_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_kitsu_5_cozy_desk_distracted
                }
                else -> when (phase) {
                    FocusPhase.Focused -> R.drawable.focus_scene_kitsu_cozy_desk
                    FocusPhase.Refocus -> R.drawable.focus_scene_kitsu_cozy_desk_refocus
                    FocusPhase.Distracted -> R.drawable.focus_scene_kitsu_cozy_desk_distracted
                }
            }
        }
    }
}

private fun focusSceneStage(species: String, stage: Int): Int =
    when (species) {
        "lumi" -> when {
            stage <= 1 -> 1
            stage >= 4 -> 4
            else -> 3
        }
        else -> when {
            stage <= 1 -> 1
            stage >= 5 -> 5
            else -> 3
        }
    }

private fun phaseLabel(phase: FocusPhase): String =
    when (phase) {
        FocusPhase.Focused -> "Focused"
        FocusPhase.Refocus -> "Refocus"
        FocusPhase.Distracted -> "Distracted"
    }

private fun phaseMessage(phase: FocusPhase): String =
    when (phase) {
        FocusPhase.Focused -> "Progress earning normally"
        FocusPhase.Refocus -> "Refocus now to keep progress smooth"
        FocusPhase.Distracted -> "Timer paused while distracted"
    }
