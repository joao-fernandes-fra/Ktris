package engine.controller.defaults

import engine.model.MoveType
import engine.model.ScoringRuleBook
import engine.model.Drop
import engine.model.SpinType
import engine.model.defaults.TetrisMoveType


open class DefaultRulebook : ScoringRuleBook {

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

    override fun getBasePoints(action: SpinType, lines: Int) = when (action) {
        SpinType.NONE -> lineClearTable[lines] ?: 0.0
        SpinType.FULL -> tSpinTable[lines] ?: 0.0
        else -> 0.0
    }

    override fun isDifficult(action: SpinType, lines: Int) =
        action == SpinType.FULL || lines == 4

    override fun getMoveType(action: SpinType, lines: Int): MoveType = when (action) {
        SpinType.NONE -> when (lines) {
            1 -> TetrisMoveType.SINGLE; 2 -> TetrisMoveType.DOUBLE; 3 -> TetrisMoveType.TRIPLE; 4 -> TetrisMoveType.TETRIS
            else -> TetrisMoveType.NONE
        }

        SpinType.FULL -> when (lines) {
            1 -> TetrisMoveType.T_SPIN_SINGLE; 2 -> TetrisMoveType.T_SPIN_DOUBLE; 3 -> TetrisMoveType.T_SPIN_TRIPLE
            else -> TetrisMoveType.NONE
        }

        SpinType.MINI -> when (lines) {
            1 -> TetrisMoveType.T_SPIN_MINI_SINGLE; 2 -> TetrisMoveType.T_SPIN_MINI_DOUBLE
            else -> TetrisMoveType.NONE
        }
    }

    override val perfectClearBonus = BONUS_PERFECT_CLEAR
    override val comboFactor = FACTOR_COMBO
    override val dropTables = mapOf(
        Drop.SOFT_DROP to MULTIPLIER_SOFT_DROP,
        Drop.HARD_DROP to MULTIPLIER_HARD_DROP
    )
}