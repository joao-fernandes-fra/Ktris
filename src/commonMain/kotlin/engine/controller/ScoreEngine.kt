package engine.controller

import engine.controller.defaults.scoring.ScoringResult
import engine.model.GameStats
import engine.model.SpinType

interface ScoreEngine {
    fun calculate(
        spinType: SpinType,
        lines: Int,
        stats: GameStats,
        isBoardEmpty: Boolean,
        pieceName: String
    ): ScoringResult
}