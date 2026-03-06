package engine.model


interface MoveType {
    val isSpecial: Boolean
    val id: String
    val displayName: String
}

interface ScoringRuleBook {
    fun getBasePoints(action: SpinType, lines: Int): Double

    fun getMoveType(action: SpinType, lines: Int): MoveType

    fun isDifficult(action: SpinType, lines: Int): Boolean

    val perfectClearBonus: Double
    val comboFactor: Double
    val dropTables: Map<Drop, Double>
}