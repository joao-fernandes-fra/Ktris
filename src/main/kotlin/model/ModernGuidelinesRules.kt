package model

enum class TetrisMoveType(
    override val isSpecial: Boolean = false,
    override val displayName: String
) : MoveType {
    NONE(displayName = ""),
    SINGLE(displayName = "Single"),
    DOUBLE(displayName = "Double"),
    TRIPLE(displayName = "Triple"),
    TETRIS(true, "Tetris"),

    T_SPIN_MINI_SINGLE(true, "T-Spin Mini Single"),
    T_SPIN_MINI_DOUBLE(true, "T-Spin Mini Double"),
    T_SPIN_SINGLE(true, "T-Spin Single"),
    T_SPIN_DOUBLE(true, "T-Spin Double"),
    T_SPIN_TRIPLE(true, "T-Spin Triple");

    override val id: String get() = name
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

    override fun getBasePoints(action: PieceAction, lines: Int) = when (action) {
        PieceAction.REGULAR -> lineClearTable[lines] ?: 0.0
        PieceAction.SPIN -> tSpinTable[lines] ?: 0.0
        else -> 0.0
    }

    override fun isDifficult(action: PieceAction, lines: Int) =
        action == PieceAction.SPIN || lines == 4

    override fun getMoveType(action: PieceAction, lines: Int): MoveType = when (action) {
        PieceAction.REGULAR -> when (lines) {
            1 -> TetrisMoveType.SINGLE; 2 -> TetrisMoveType.DOUBLE; 3 -> TetrisMoveType.TRIPLE; 4 -> TetrisMoveType.TETRIS
            else -> TetrisMoveType.NONE
        }

        PieceAction.SPIN -> when (lines) {
            1 -> TetrisMoveType.T_SPIN_SINGLE; 2 -> TetrisMoveType.T_SPIN_DOUBLE; 3 -> TetrisMoveType.T_SPIN_TRIPLE
            else -> TetrisMoveType.NONE
        }

        PieceAction.MINI_SPIN -> when (lines) {
            1 -> TetrisMoveType.T_SPIN_MINI_SINGLE; 2 -> TetrisMoveType.T_SPIN_MINI_DOUBLE;
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