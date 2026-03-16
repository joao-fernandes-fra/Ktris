package engine.controller.defaults.scoring

import engine.model.Drop
import engine.model.MoveType
import engine.model.ScoringRuleBook
import engine.model.SpinType
import engine.model.defaults.DefaultMoveType
import engine.model.defaults.SpinMoveType


open class DefaultRulebook : ScoringRuleBook {

    companion object {
        private const val POINTS_SINGLE = 100.0
        private const val POINTS_DOUBLE = 300.0
        private const val POINTS_TRIPLE = 500.0
        private const val POINTS_TETRIS = 800.0

        private const val POINTS_SPIN_ZERO = 400.0
        private const val POINTS_SPIN_ONE = 800.0
        private const val POINTS_SPIN_TWO = 1200.0
        private const val POINTS_SPIN_THREE = 1600.0

        private const val POINTS_MINI_ZERO = 100.0
        private const val POINTS_MINI_ONE = 200.0
        private const val POINTS_MINI_TWO = 400.0

        private const val BONUS_PERFECT_CLEAR = 2000.0
        private const val FACTOR_COMBO = 50.0
        private const val MULTIPLIER_SOFT_DROP = 1.0
        private const val MULTIPLIER_HARD_DROP = 2.0
    }

    private val lineClearTable = mapOf(
        1 to POINTS_SINGLE,
        2 to POINTS_DOUBLE,
        3 to POINTS_TRIPLE,
        4 to POINTS_TETRIS
    )

    private val spinTable = mapOf(
        0 to POINTS_SPIN_ZERO,
        1 to POINTS_SPIN_ONE,
        2 to POINTS_SPIN_TWO,
        3 to POINTS_SPIN_THREE
    )

    private val miniSpinTable = mapOf(
        0 to POINTS_MINI_ZERO,
        1 to POINTS_MINI_ONE,
        2 to POINTS_MINI_TWO
    )

    override fun getBasePoints(spinType: SpinType, lines: Int) = when (spinType) {
        SpinType.NONE -> lineClearTable[lines] ?: 0.0
        SpinType.FULL -> spinTable[lines] ?: 0.0
        SpinType.MINI -> miniSpinTable[lines] ?: 0.0
    }

    override fun isDifficult(spinType: SpinType, lines: Int) = when (spinType) {
        SpinType.FULL -> true
        SpinType.MINI -> lines >= 1
        SpinType.NONE -> lines == 4
    }

    override fun getMoveType(spinType: SpinType, lines: Int, pieceName: String): MoveType =
        when (spinType) {
            SpinType.NONE -> when (lines) {
                1 -> DefaultMoveType.SINGLE
                2 -> DefaultMoveType.DOUBLE
                3 -> DefaultMoveType.TRIPLE
                4 -> DefaultMoveType.QUAD
                else -> DefaultMoveType.NONE
            }

            SpinType.FULL, SpinType.MINI -> SpinMoveType(pieceName, spinType, lines)
        }

    override val perfectClearBonus = BONUS_PERFECT_CLEAR
    override val comboFactor = FACTOR_COMBO
    override val dropTables = mapOf(
        Drop.SOFT_DROP to MULTIPLIER_SOFT_DROP,
        Drop.HARD_DROP to MULTIPLIER_HARD_DROP
    )
}