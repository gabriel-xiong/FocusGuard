package com.focusguard.pet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PetScreen(
    modifier: Modifier = Modifier,
    viewModel: PetViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        // Main pet display
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(
                    PetAssets.spriteRes(state.species, state.currentStage)
                ),
                contentDescription = state.petName,
                modifier = Modifier.size(220.dp),
                contentScale = ContentScale.Fit,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = state.petName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "${EvolutionThresholds.stageName(state.currentStage)} (Stage ${state.currentStage}/${PetAssets.MAX_STAGE})",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            Text(
                text = "${state.lifetimeFocusMinutes}m lifetime focus",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            )
        }

        // Evolution cutscene overlay
        AnimatedVisibility(
            visible = state.cutscene != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            state.cutscene?.let { cutsceneData ->
                EvolutionCutscene(
                    data = cutsceneData,
                    onDismiss = viewModel::dismissCutscene,
                )
            }
        }
    }
}
