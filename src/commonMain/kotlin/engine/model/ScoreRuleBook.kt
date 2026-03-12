package engine.model


interface ScoringRuleBook {
    fun getBasePoints(spinType: SpinType, lines: Int): Double
    fun getMoveType(spinType: SpinType, lines: Int, pieceName: String): MoveType
    fun isDifficult(spinType: SpinType, lines: Int): Boolean

    val perfectClearBonus: Double
    val comboFactor: Double
    val dropTables: Map<Drop, Double>
    val b2bMultiplier: Double get() = 1.5
    val startingLevel: Int get() = 1
    val maxLevel: Int? get() = null
    val linesPerLevel: Int? get() = 10

    fun levelForLines(linesCleared: Int, currentLevel: Int): Int {
        val lpl = linesPerLevel ?: return currentLevel
        val newLevel = startingLevel + (linesCleared / lpl)
        return if (maxLevel != null) minOf(newLevel, maxLevel!!) else newLevel
    }
}

interface MoveType {
    val isSpecial: Boolean
    val id: String
    val displayName: String
}