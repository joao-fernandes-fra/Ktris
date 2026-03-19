package engine.controller.defaults.scoring

import engine.controller.ScoreEngine
import engine.model.GameStats
import engine.model.MoveType
import engine.model.ScoringRuleBook
import engine.model.SpinType

class DefaultScoreEngine(private val ruleBook: ScoringRuleBook) : ScoreEngine {

    override fun calculate(
        spinType: SpinType,
        lines: Int,
        stats: GameStats,
        isBoardEmpty: Boolean,
        pieceName: String
    ): ScoringResult {
        var basePoints = ruleBook.getBasePoints(spinType, lines)
        val isDifficult = ruleBook.isDifficult(spinType, lines)

        val newB2b = when {
            isDifficult -> stats.b2bCount + 1
            lines >= 1 -> -1
            else -> stats.b2bCount
        }

        if (isDifficult && newB2b > 0) basePoints *= ruleBook.b2bMultiplier

        val newCombo = if (lines > 0) stats.combo + 1 else -1
        val comboBonus = if (newCombo > 0) ruleBook.comboFactor * newCombo * stats.level else 0.0
        val pcBonus = if (isBoardEmpty) ruleBook.perfectClearBonus * stats.level else 0.0
        val pointsAwarded = (basePoints * stats.level) + comboBonus + pcBonus


        val moveType = ruleBook.getMoveType(spinType, lines, pieceName)
        return ScoringResult(
            pointsAwarded = pointsAwarded,
            newCombo = newCombo,
            newB2b = newB2b,
            linesCleared = lines,
            moveType = moveType
        )
    }
}

data class ScoringResult(
    val pointsAwarded: Double,
    val newCombo: Int,
    val newB2b: Int,
    val linesCleared: Int,
    val moveType: MoveType,
)