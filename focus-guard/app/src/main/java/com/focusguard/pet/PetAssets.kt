package com.focusguard.pet

import androidx.annotation.DrawableRes
import com.focusguard.R

enum class PetSpecies(val id: String, val displayName: String) {
    KITSU("kitsu", "Kitsu"),
    OWLY("owly", "Owly"),
    LUMI("lumi", "Lumi");

    companion object {
        fun fromId(id: String): PetSpecies =
            entries.firstOrNull { it.id == id } ?: KITSU
    }
}

object PetAssets {

    private val sprites: Map<PetSpecies, Map<Int, Int>> = mapOf(
        PetSpecies.KITSU to mapOf(
            1 to R.drawable.kitsu_1,
            3 to R.drawable.kitsu_3,
            5 to R.drawable.kitsu_5,
        ),
        PetSpecies.OWLY to mapOf(
            1 to R.drawable.owly_1,
            3 to R.drawable.owly_3,
            5 to R.drawable.owly_5,
        ),
        PetSpecies.LUMI to mapOf(
            1 to R.drawable.lumi_1,
            3 to R.drawable.lumi_3,
            4 to R.drawable.lumi_4,
        ),
    )

    const val MIN_STAGE = 1
    const val MAX_STAGE = 5

    @DrawableRes
    fun spriteRes(species: PetSpecies, stage: Int): Int {
        return sprites.getValue(species).getValue(displayStage(species, stage))
    }

    @DrawableRes
    fun spriteRes(speciesId: String, stage: Int): Int =
        spriteRes(PetSpecies.fromId(speciesId), stage)

    fun availableStages(species: PetSpecies): List<Int> =
        sprites.getValue(species).keys.sorted()

    fun displayStage(species: PetSpecies, stage: Int): Int {
        val stages = availableStages(species)
        return stages.lastOrNull { stage >= it } ?: stages.first()
    }

    fun nextDisplayStage(species: PetSpecies, unlockedStage: Int): Int =
        availableStages(species).lastOrNull { unlockedStage >= it } ?: MIN_STAGE

    /**
     * Maps an internal stage number to a 1-based display number.
     * e.g. for kitsu stages [1, 3, 5] → display numbers 1, 2, 3.
     */
    fun displayNumber(species: PetSpecies, internalStage: Int): Int {
        val stages = availableStages(species)
        val index = stages.indexOf(internalStage)
        return if (index >= 0) index + 1 else stages.size
    }

    /** Total number of available stages for a species. */
    fun stageCount(species: PetSpecies): Int = availableStages(species).size

    /**
     * Returns the internal stage threshold required to unlock the given internal stage.
     * Each stage unlocks at (internalStage - 1) * 120 focus minutes.
     */
    fun minutesRequiredForStage(internalStage: Int): Int =
        ((internalStage - 1) * 120).coerceAtLeast(0)
}
