package com.focusguard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.pet.PetAssets
import com.focusguard.pet.PetSpecies
import com.focusguard.state.PetProfile
import com.focusguard.ui.theme.EarnedColors

private data class PetVariant(
    val species: PetSpecies,
    val stage: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetCollectionBottomSheet(
    currentPet: PetProfile,
    unlockedPetSpecies: List<String>,
    totalPoints: Int,
    onDismiss: () -> Unit,
    onSelectPet: (PetSpecies, Int) -> Unit,
    onUnlockPet: (PetSpecies, Int) -> Boolean,
) {
    val unlockedStage = unlockedStageFor(totalPoints)
    val ownedSpecies = remember(unlockedPetSpecies, currentPet.species) {
        (unlockedPetSpecies + currentPet.species).distinct()
    }

    val allVariants = remember {
        PetSpecies.entries.flatMap { species ->
            PetAssets.availableStages(species).map { stage ->
                PetVariant(species, stage)
            }
        }
    }

    var selected by remember {
        mutableStateOf(
            PetVariant(
                PetSpecies.fromId(currentPet.species),
                PetAssets.displayStage(PetSpecies.fromId(currentPet.species), currentPet.stage)
            )
        )
    }

    val selectedUnlocked = ownedSpecies.contains(selected.species.id) && selected.stage <= unlockedStage
    val selectedTone = speciesTone(selected.species)
    val isCurrentPet = selected.species.id == currentPet.species &&
        selected.stage == PetAssets.displayStage(PetSpecies.fromId(currentPet.species), currentPet.stage)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .size(width = 50.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.42f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            // Preview of selected pet
            SelectedPreview(
                species = selected.species,
                stage = selected.stage,
                unlocked = selectedUnlocked,
                tone = selectedTone,
                isCurrentPet = isCurrentPet,
            )

            Spacer(Modifier.height(12.dp))

            // Progress bar toward next evolution
            EvolutionProgressBar(
                totalPoints = totalPoints,
                unlockedStage = unlockedStage,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Choose your study buddy",
                fontFamily = FontFamily.Serif,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap a pet to preview, then select to make it yours.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            )

            Spacer(Modifier.height(14.dp))

            // Flat grid of all pet variants
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(allVariants, key = { "${it.species.id}_${it.stage}" }) { variant ->
                    val variantUnlocked = ownedSpecies.contains(variant.species.id) && variant.stage <= unlockedStage
                    val isSelected = variant == selected
                    val tone = speciesTone(variant.species)
                    val displayNum = PetAssets.displayNumber(variant.species, variant.stage)

                    PetTile(
                        species = variant.species,
                        displayStageNumber = displayNum,
                        internalStage = variant.stage,
                        selected = isSelected,
                        unlocked = variantUnlocked,
                        isCurrent = variant.species.id == currentPet.species &&
                            variant.stage == PetAssets.displayStage(
                                PetSpecies.fromId(currentPet.species), currentPet.stage
                            ),
                        tone = tone,
                        onClick = { selected = variant },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action button
            val unlockCost = speciesUnlockCost(selected.species)
            val needsUnlock = !ownedSpecies.contains(selected.species.id)
            val stageLocked = ownedSpecies.contains(selected.species.id) && selected.stage > unlockedStage
            val displayNum = PetAssets.displayNumber(selected.species, selected.stage)

            Button(
                onClick = {
                    when {
                        needsUnlock -> {
                            if (onUnlockPet(selected.species, unlockCost)) {
                                onSelectPet(selected.species, selected.stage)
                            }
                        }
                        else -> onSelectPet(selected.species, selected.stage)
                    }
                },
                enabled = when {
                    isCurrentPet -> false
                    needsUnlock -> totalPoints >= unlockCost
                    stageLocked -> false
                    else -> true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedTone,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            ) {
                Text(
                    text = when {
                        isCurrentPet -> "Current buddy"
                        stageLocked -> "Stage locked · earn more points"
                        needsUnlock && totalPoints < unlockCost ->
                            "Need ${unlockCost - totalPoints} more pts"
                        needsUnlock -> "Unlock & select · %,d pts".format(unlockCost)
                        else -> "Select ${selected.species.displayName} Stage $displayNum"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun EvolutionProgressBar(
    totalPoints: Int,
    unlockedStage: Int,
) {
    val nextThreshold = com.focusguard.state.pointsForNextStage(unlockedStage)
    if (nextThreshold == null) {
        // All stages unlocked
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = EarnedColors.Focus.copy(alpha = 0.10f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "All evolutions unlocked!",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EarnedColors.Focus,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "%,d pts".format(totalPoints),
                    fontSize = 12.sp,
                    color = EarnedColors.Focus.copy(alpha = 0.7f),
                )
            }
        }
    } else {
        val currentThreshold = com.focusguard.state.pointsRequiredForStage(unlockedStage)
        val pointsRemaining = (nextThreshold - totalPoints).coerceAtLeast(0)
        val range = (nextThreshold - currentThreshold).coerceAtLeast(1)
        val progressInRange = (totalPoints - currentThreshold).coerceAtLeast(0)
        val progressFraction = (progressInRange.toFloat() / range).coerceIn(0f, 1f)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = EarnedColors.Primary.copy(alpha = 0.06f),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "%,d pts to next evolution".format(pointsRemaining),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "%,d / %,d".format(totalPoints, nextThreshold),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = EarnedColors.Primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun SelectedPreview(
    species: PetSpecies,
    stage: Int,
    unlocked: Boolean,
    tone: Color,
    isCurrentPet: Boolean,
) {
    val displayNum = PetAssets.displayNumber(species, stage)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = tone.copy(alpha = 0.06f),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painter = painterResource(PetAssets.spriteRes(species, stage)),
                contentDescription = species.displayName,
                modifier = Modifier
                    .size(88.dp)
                    .alpha(if (unlocked) 1f else 0.35f),
                contentScale = ContentScale.Fit,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    species.displayName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Stage $displayNum of ${PetAssets.stageCount(species)}",
                    fontSize = 14.sp,
                    color = tone,
                    fontWeight = FontWeight.SemiBold,
                )
                if (isCurrentPet) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Your current buddy",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!unlocked) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Locked",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PetTile(
    species: PetSpecies,
    displayStageNumber: Int,
    internalStage: Int,
    selected: Boolean,
    unlocked: Boolean,
    isCurrent: Boolean,
    tone: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = when {
                selected -> tone.copy(alpha = 0.10f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
            },
            border = when {
                selected -> BorderStroke(2.dp, tone)
                isCurrent -> BorderStroke(1.dp, tone.copy(alpha = 0.4f))
                else -> null
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(PetAssets.spriteRes(species, internalStage)),
                    contentDescription = "${species.displayName} stage $displayStageNumber",
                    modifier = Modifier
                        .padding(8.dp)
                        .size(62.dp)
                        .alpha(if (unlocked) 1f else 0.22f),
                    contentScale = ContentScale.Fit,
                )
                if (!unlocked) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            species.displayName,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) tone else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Text(
            "Stage $displayStageNumber",
            fontSize = 10.sp,
            color = if (selected) tone.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
        )
    }
}

private fun unlockedStageFor(totalPoints: Int): Int =
    com.focusguard.state.petStageForPoints(totalPoints)

private fun speciesUnlockCost(species: PetSpecies): Int =
    com.focusguard.state.SPECIES_UNLOCK_COST[species.id] ?: 0

private fun speciesTone(species: PetSpecies): Color =
    when (species) {
        PetSpecies.KITSU -> EarnedColors.Primary
        PetSpecies.OWLY -> EarnedColors.Secondary
        PetSpecies.LUMI -> EarnedColors.Focus
    }
