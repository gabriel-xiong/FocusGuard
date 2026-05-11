package com.focusguard.pet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Configurable timing constants for the evolution cutscene.
 * All values in milliseconds.
 */
object EvolutionTiming {
    const val BACKDROP_FADE_IN = 400L
    const val SHAKE_START = 600L
    const val SHAKE_DURATION = 2000L
    const val SILHOUETTE_START = 1400L
    const val PULSE_START = 2400L
    const val FLASH_START = 3400L
    const val REVEAL_START = 3800L
    const val AUTO_DISMISS = 5200L
    const val SPRITE_SIZE_DP = 220
}

/**
 * Full-screen evolution cutscene inspired by the reference web animation.
 *
 * Sequence (~5.2s):
 *   0.0s  Backdrop fades in, "What?!" text
 *   0.6s  Old sprite appears, starts shaking
 *   1.4s  Sprite turns into white silhouette, light rays bloom
 *   2.4s  Silhouette pulses faster, energy ring contracts
 *   3.4s  FLASH -- brightness spike, scale burst
 *   3.8s  New sprite revealed with "evolved into..." text
 *   5.2s  Auto-dismiss
 */
@Composable
fun EvolutionCutscene(
    data: EvolutionCutsceneData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var phase by remember { mutableStateOf(CutscenePhase.INTRO) }

    // Phase progression driven by delays
    LaunchedEffect(Unit) {
        // INTRO -> SHAKE
        delay(EvolutionTiming.SHAKE_START)
        phase = CutscenePhase.SHAKE

        // SHAKE -> SILHOUETTE
        delay(EvolutionTiming.SILHOUETTE_START - EvolutionTiming.SHAKE_START)
        phase = CutscenePhase.SILHOUETTE

        // SILHOUETTE -> PULSE
        delay(EvolutionTiming.PULSE_START - EvolutionTiming.SILHOUETTE_START)
        phase = CutscenePhase.PULSE

        // PULSE -> FLASH
        delay(EvolutionTiming.FLASH_START - EvolutionTiming.PULSE_START)
        phase = CutscenePhase.FLASH

        // FLASH -> REVEAL
        delay(EvolutionTiming.REVEAL_START - EvolutionTiming.FLASH_START)
        phase = CutscenePhase.REVEAL

        // Auto-dismiss
        delay(EvolutionTiming.AUTO_DISMISS - EvolutionTiming.REVEAL_START)
        onDismiss()
    }

    // --- Animated values ---

    // Shake oscillation
    val shakeTransition = rememberInfiniteTransition(label = "shake")
    val shakeOffset by shakeTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shakeX",
    )

    // Flash burst scale
    val flashScale = remember { Animatable(0.2f) }
    val flashAlpha = remember { Animatable(0f) }
    LaunchedEffect(phase) {
        if (phase == CutscenePhase.FLASH) {
            flashAlpha.animateTo(1f, tween(100))
            flashScale.animateTo(2.4f, tween(500, easing = FastOutSlowInEasing))
            flashAlpha.animateTo(0f, tween(300))
        }
    }

    // Old sprite fade/scale
    val oldSpriteAlpha = remember { Animatable(0f) }
    val oldSpriteScale = remember { Animatable(0.9f) }
    LaunchedEffect(phase) {
        when (phase) {
            CutscenePhase.SHAKE -> {
                oldSpriteAlpha.animateTo(1f, tween(300))
                oldSpriteScale.animateTo(1f, tween(300))
            }
            CutscenePhase.SILHOUETTE -> {
                oldSpriteScale.animateTo(1.05f, tween(800, easing = FastOutSlowInEasing))
            }
            CutscenePhase.FLASH -> {
                oldSpriteAlpha.animateTo(0f, tween(100))
            }
            else -> {}
        }
    }

    // New sprite reveal
    val newSpriteAlpha = remember { Animatable(0f) }
    val newSpriteScale = remember { Animatable(0.4f) }
    LaunchedEffect(phase) {
        if (phase == CutscenePhase.REVEAL) {
            newSpriteAlpha.animateTo(1f, tween(300))
            newSpriteScale.animateTo(1.15f, tween(250, easing = FastOutSlowInEasing))
            newSpriteScale.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        }
    }

    // Energy ring
    val ringScale = remember { Animatable(2.2f) }
    val ringAlpha = remember { Animatable(0f) }
    LaunchedEffect(phase) {
        if (phase == CutscenePhase.PULSE) {
            ringAlpha.animateTo(0.8f, tween(200))
            ringScale.animateTo(0.6f, tween(900, easing = FastOutSlowInEasing))
            ringAlpha.animateTo(0f, tween(200))
        }
    }

    // Backdrop alpha
    val backdropAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        backdropAlpha.animateTo(1f, tween(EvolutionTiming.BACKDROP_FADE_IN.toInt()))
    }

    val isSilhouetted = phase == CutscenePhase.SILHOUETTE || phase == CutscenePhase.PULSE
    val showShake = phase == CutscenePhase.SHAKE || phase == CutscenePhase.SILHOUETTE || phase == CutscenePhase.PULSE
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(backdropAlpha.value)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        Color.Black.copy(alpha = 0.92f),
                        Color.Black,
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // "What?!" intro text
        AnimatedVisibility(
            visible = phase.ordinal < CutscenePhase.FLASH.ordinal,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset { IntOffset(0, 160) },
            ) {
                Text(
                    text = "What?!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${data.petName} is evolving!",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }

        // Sprite container
        Box(
            modifier = Modifier.size(EvolutionTiming.SPRITE_SIZE_DP.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Energy ring
            Box(
                modifier = Modifier
                    .size(EvolutionTiming.SPRITE_SIZE_DP.dp)
                    .scale(ringScale.value)
                    .alpha(ringAlpha.value)
                    .drawBehind {
                        drawCircle(
                            color = primaryColor,
                            radius = size.minDimension / 2,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 4.dp.toPx()
                            ),
                        )
                    }
            )

            // Old sprite
            if (phase != CutscenePhase.REVEAL && phase != CutscenePhase.INTRO) {
                Image(
                    painter = painterResource(
                        PetAssets.spriteRes(data.species, data.fromStage)
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(EvolutionTiming.SPRITE_SIZE_DP.dp)
                        .alpha(oldSpriteAlpha.value)
                        .scale(oldSpriteScale.value)
                        .offset {
                            IntOffset(
                                x = if (showShake) shakeOffset.toInt() else 0,
                                y = 0,
                            )
                        },
                    contentScale = ContentScale.Fit,
                    colorFilter = if (isSilhouetted) ColorFilter.tint(Color.White) else null,
                )
            }

            // Flash burst
            Box(
                modifier = Modifier
                    .size((EvolutionTiming.SPRITE_SIZE_DP * 1.8f).dp)
                    .scale(flashScale.value)
                    .alpha(flashAlpha.value)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                primaryColor.copy(alpha = 0.6f),
                                Color.Transparent,
                            )
                        ),
                        CircleShape,
                    )
            )

            // New sprite (revealed)
            if (phase == CutscenePhase.REVEAL) {
                Image(
                    painter = painterResource(
                        PetAssets.spriteRes(data.species, data.toStage)
                    ),
                    contentDescription = "${data.petName} evolved",
                    modifier = Modifier
                        .size(EvolutionTiming.SPRITE_SIZE_DP.dp)
                        .alpha(newSpriteAlpha.value)
                        .scale(newSpriteScale.value),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        // "Evolved into..." outro text
        AnimatedVisibility(
            visible = phase == CutscenePhase.REVEAL,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset { IntOffset(0, -200) },
            ) {
                Text(
                    text = "EVOLVED INTO",
                    fontSize = 10.sp,
                    letterSpacing = 3.sp,
                    color = Color.White.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = EvolutionThresholds.stageName(data.toStage),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = data.species.displayName,
                    fontSize = 14.sp,
                    color = primaryColor,
                )
            }
        }

        // Tap hint
        AnimatedVisibility(
            visible = phase == CutscenePhase.REVEAL,
            enter = fadeIn(tween(600)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, -48) },
        ) {
            Text(
                text = "TAP TO DISMISS",
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.4f),
            )
        }
    }
}

private enum class CutscenePhase {
    INTRO,
    SHAKE,
    SILHOUETTE,
    PULSE,
    FLASH,
    REVEAL,
}
