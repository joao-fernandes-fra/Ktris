package controller

import model.Drop
import model.SpinType


enum class PieceAction {
    SPIN, MINI_SPIN, REGULAR;

    companion object {
        fun mapAction(spinType: SpinType): PieceAction {
            return when (spinType) {
                SpinType.FULL -> SPIN
                SpinType.MINI -> MINI_SPIN
                SpinType.NONE -> REGULAR
            }
        }
    }
}

interface MoveType {
    val isSpecial: Boolean
    val id: String
    val displayName: String
}

interface ScoringRuleBook {
    fun getBasePoints(action: PieceAction, lines: Int): Double

    fun getMoveType(action: PieceAction, lines: Int): MoveType

    fun isDifficult(action: PieceAction, lines: Int): Boolean

    val perfectClearBonus: Double
    val comboFactor: Double
    val dropTables: Map<Drop, Double>
}