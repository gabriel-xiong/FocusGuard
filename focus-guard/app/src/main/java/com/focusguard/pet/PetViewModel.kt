package com.focusguard.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.state.EarnedItStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class PetViewModel : ViewModel() {

    private val _cutscene = MutableStateFlow<EvolutionCutsceneData?>(null)
    private var lastKnownStage: Int? = null

    val uiState: StateFlow<PetEvolutionState> = combine(
        EarnedItStore.state,
        _cutscene,
    ) { appState, cutscene ->
        val pet = appState.pet
        val species = PetSpecies.fromId(pet.species)
        val currentStage = pet.stage.coerceIn(PetAssets.MIN_STAGE, PetAssets.MAX_STAGE)

        // Detect stage increase and trigger cutscene
        val prev = lastKnownStage
        if (prev != null && currentStage > prev && cutscene == null) {
            _cutscene.value = EvolutionCutsceneData(
                fromStage = prev,
                toStage = currentStage,
                species = species,
                petName = pet.name,
            )
        }
        lastKnownStage = currentStage

        PetEvolutionState(
            species = species,
            petName = pet.name,
            currentStage = currentStage,
            lifetimeFocusMinutes = appState.lifetimeFocusMinutes,
            cutscene = cutscene,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PetEvolutionState(),
    )

    fun dismissCutscene() {
        _cutscene.value = null
    }
}
