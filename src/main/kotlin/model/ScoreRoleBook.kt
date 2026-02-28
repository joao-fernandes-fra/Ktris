package model

import util.TSpinType

enum class ClearAction {
    T_SPIN, T_SPIN_MINI, LINE_CLEAR;

    companion object {
        fun mapAction(spinType: TSpinType): ClearAction {
            return when (spinType) {
                TSpinType.REGULAR -> T_SPIN
                TSpinType.MINI -> T_SPIN_MINI
                TSpinType.NONE -> LINE_CLEAR
            }
        }
    }
}

interface ScoringRuleBook {
    fun getBasePoints(action: ClearAction, lines: Int): Double

    fun getMoveType(action: ClearAction, lines: Int): MoveType

    fun isDifficult(action: ClearAction, lines: Int): Boolean

    val perfectClearBonus: Double
    val comboFactor: Double
    val dropTables: Map<Drop, Double>
}

class ModernGuidelineRules : ScoringRuleBook {

    companion object {
        // --- Line Clear Points ---
        private const val POINTS_SINGLE = 100.0
        private const val POINTS_DOUBLE = 300.0
        private const val POINTS_TRIPLE = 500.0
        private const val POINTS_TETRIS = 800.0

        // --- T-Spin Points ---
        private const val POINTS_T_SPIN_ZERO = 400.0
        private const val POINTS_T_SPIN_SINGLE = 800.0
        private const val POINTS_T_SPIN_DOUBLE = 1200.0
        private const val POINTS_T_SPIN_TRIPLE = 1600.0

        // --- Bonuses & Factors ---
        private const val BONUS_PERFECT_CLEAR = 2000.0
        private const val FACTOR_COMBO = 50.0

        // --- Multipliers ---
        private const val MULTIPLIER_SOFT_DROP = 1.0
        private const val MULTIPLIER_HARD_DROP = 2.0
    }

    private val lineClearTable = mapOf(
        1 to POINTS_SINGLE,
        2 to POINTS_DOUBLE,
        3 to POINTS_TRIPLE,
        4 to POINTS_TETRIS
    )

    private val tSpinTable = mapOf(
        0 to POINTS_T_SPIN_ZERO,
        1 to POINTS_T_SPIN_SINGLE,
        2 to POINTS_T_SPIN_DOUBLE,
        3 to POINTS_T_SPIN_TRIPLE
    )

    override fun getBasePoints(action: ClearAction, lines: Int) = when (action) {
        ClearAction.LINE_CLEAR -> lineClearTable[lines] ?: 0.0
        ClearAction.T_SPIN -> tSpinTable[lines] ?: 0.0
        else -> 0.0
    }

    override fun isDifficult(action: ClearAction, lines: Int) =
        action == ClearAction.T_SPIN || lines == 4

    override fun getMoveType(action: ClearAction, lines: Int): MoveType = when (action) {
        ClearAction.LINE_CLEAR -> when (lines) {
            1 -> MoveType.SINGLE; 2 -> MoveType.DOUBLE; 3 -> MoveType.TRIPLE; 4 -> MoveType.TETRIS
            else -> MoveType.NONE
        }

        ClearAction.T_SPIN -> when (lines) {
            1 -> MoveType.T_SPIN_SINGLE; 2 -> MoveType.T_SPIN_DOUBLE; 3 -> MoveType.T_SPIN_TRIPLE
            else -> MoveType.NONE
        }

        ClearAction.T_SPIN_MINI -> when (lines) {
            1 -> MoveType.T_SPIN_MINI_SINGLE; 2 -> MoveType.T_SPIN_MINI_DOUBLE;
            else -> MoveType.NONE
        }
    }

    override val perfectClearBonus = BONUS_PERFECT_CLEAR
    override val comboFactor = FACTOR_COMBO
    override val dropTables = mapOf(
        Drop.SOFT_DROP to MULTIPLIER_SOFT_DROP,
        Drop.HARD_DROP to MULTIPLIER_HARD_DROP
    )
}