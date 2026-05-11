package com.focusguard.pet

data class PetEvolutionState(
    val species: PetSpecies = PetSpecies.KITSU,
    val petName: String = "Kitsu",
    val currentStage: Int = 1,
    val lifetimeFocusMinutes: Int = 0,
    val cutscene: EvolutionCutsceneData? = null,
)

data class EvolutionCutsceneData(
    val fromStage: Int,
    val toStage: Int,
    val species: PetSpecies,
    val petName: String,
)

object EvolutionThresholds {

    /** Points required to reach each stage. */
    val STAGE_POINTS: Map<Int, Int> = mapOf(
        1 to 0,
        3 to 500,
        5 to 1_500,
    )

    fun stageForPoints(totalPoints: Int): Int {
        var stage = 1
        for ((s, threshold) in STAGE_POINTS) {
            if (totalPoints >= threshold) stage = s
        }
        return stage.coerceIn(PetAssets.MIN_STAGE, PetAssets.MAX_STAGE)
    }

    fun pointsForNextStage(currentStage: Int): Int? {
        val sorted = STAGE_POINTS.keys.sorted()
        val nextKey = sorted.firstOrNull { it > currentStage }
        return nextKey?.let { STAGE_POINTS[it] }
    }

    val STAGE_NAMES: Map<Int, String> = mapOf(
        1 to "Hatchling",
        2 to "Sprout",
        3 to "Scout",
        4 to "Guardian",
        5 to "Champion",
    )

    fun stageName(stage: Int): String =
        STAGE_NAMES[stage.coerceIn(PetAssets.MIN_STAGE, PetAssets.MAX_STAGE)] ?: "Unknown"
}
