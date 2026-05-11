package com.focusguard.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.R
import com.focusguard.state.EarnedItStore
import com.focusguard.state.PetProfile
import com.focusguard.ui.theme.EarnedColors
import kotlinx.coroutines.launch

private val OnboardingTitleColor = Color(0xFF1F3D24)
private val OnboardingSubtitleColor = Color(0xFF1F3D24).copy(alpha = 0.65f)

private data class OnboardingPage(
    val title: String,
    val subtitle: String,
    @DrawableRes val hero: Int? = null
)

private val onboardingPages = listOf(
    OnboardingPage(
        "Earn your\ntime back",
        "Lock distractions.\nFocus deeply.\nSpend rewards later.",
        R.drawable.onboarding_welcome
    ),
    OnboardingPage(
        "Focus stays\non your phone",
        "Attention signals run locally.\nNo frames uploaded.",
        R.drawable.onboarding_privacy
    ),
    OnboardingPage(
        "Pick what\ngets locked",
        "Choose the apps\nthat pull you away.",
        R.drawable.onboarding_lock_apps
    ),
    OnboardingPage(
        "Grow a\nfocus pet",
        "Your pet evolves\nwhen sessions go well.",
        null
    ),
    OnboardingPage(
        "Spend rewards\nafter focus",
        "Focused minutes become\nearned app time.",
        R.drawable.onboarding_rewards
    )
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    var selectedSpecies by remember { mutableStateOf("kitsu") }
    var completing by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    val isLast = pagerState.currentPage == onboardingPages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EarnedColors.LightBg)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPageContent(
                data = onboardingPages[page],
                petSpecies = selectedSpecies,
                onSpecies = { selectedSpecies = it }
            )
        }

        val backVisibility by animateFloatAsState(
            targetValue = if (pagerState.currentPage > 0) 1f else 0f,
            label = "backAlpha"
        )
        Surface(
            onClick = {
                if (pagerState.currentPage > 0) {
                    haptics.tap()
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 36.dp)
                .size(40.dp)
                .alpha(backVisibility),
            enabled = pagerState.currentPage > 0,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.85f),
            shadowElevation = 1.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OnboardingTitleColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Fixed bottom bar — dots + CTA, sits over the cream-faded zone of every slide
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DotIndicator(
                count = onboardingPages.size,
                current = pagerState.currentPage
            )
            Spacer(Modifier.height(18.dp))
            Crossfade(targetState = isLast, label = "ctaLabel") { last ->
                Button(
                    onClick = {
                        if (last && !completing) {
                            completing = true
                            haptics.confirm()
                            val petName = selectedSpecies.replaceFirstChar { it.uppercase() }
                            EarnedItStore.completeOnboarding(selectedSpecies, petName)
                            onComplete()
                        } else if (!last) {
                            haptics.tap()
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EarnedColors.Primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (last) "Let's go!" else "Next",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    data: OnboardingPage,
    petSpecies: String,
    onSpecies: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (data.hero != null) {
            Image(
                painter = painterResource(id = data.hero),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(245.dp)
                    .background(
                        Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to EarnedColors.LightBg,
                                0.55f to EarnedColors.LightBg.copy(alpha = 0.98f),
                                0.76f to EarnedColors.LightBg.copy(alpha = 0.82f),
                                0.92f to EarnedColors.LightBg.copy(alpha = 0.30f),
                                1.00f to Color.Transparent
                            )
                        )
                    )
            )
            // Bottom fade — held transparent through the image, then a fast,
            // late ramp so the image is fully cream well before the dots/CTA.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.74f to Color.Transparent,
                                0.80f to EarnedColors.LightBg.copy(alpha = 0.85f),
                                0.84f to EarnedColors.LightBg,
                                1.00f to EarnedColors.LightBg
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = data.title,
                color = OnboardingTitleColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                lineHeight = 36.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = data.subtitle,
                color = OnboardingSubtitleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )

            if (data.hero == null) {
                Spacer(Modifier.height(28.dp))
                PetPickerHero(
                    species = petSpecies,
                    onSpecies = onSpecies
                )
            }

            Spacer(Modifier.weight(1f))
            // Reserve space so content doesn't slide under the fixed bottom bar
            Spacer(Modifier.height(150.dp))
        }
    }
}

@Composable
private fun DotIndicator(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(count) { index ->
            val active = index == current
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) EarnedColors.Focus
                        else OnboardingTitleColor.copy(alpha = 0.18f)
                    )
            )
        }
    }
}

@Composable
private fun PetPickerHero(
    species: String,
    onSpecies: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("kitsu", "lumi", "owly").forEach { id ->
                PetSpeciesCard(
                    id = id,
                    selected = id == species,
                    modifier = Modifier.weight(1f),
                    onClick = { onSpecies(id) }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = EarnedColors.Focus.copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Eco,
                    contentDescription = null,
                    tint = EarnedColors.Focus,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "More focus = more points\n= a happier, stronger pet.",
                    color = EarnedColors.Focus,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun PetSpeciesCard(
    id: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptics = rememberHaptics()

    Surface(
        modifier = modifier
            .height(172.dp)
            .clickable {
                haptics.select()
                onClick()
            },
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = if (selected) BorderStroke(2.dp, EarnedColors.Primary)
        else BorderStroke(1.dp, OnboardingTitleColor.copy(alpha = 0.08f)),
        shadowElevation = if (selected) 6.dp else 1.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 14.dp, bottom = 14.dp)
            ) {
                PetSprite(
                    pet = PetProfile(species = id, name = id, stage = 2),
                    size = 96.dp,
                    glow = false
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    id.replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = OnboardingTitleColor
                )
            }
            if (selected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(24.dp),
                    shape = CircleShape,
                    color = EarnedColors.Primary,
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
