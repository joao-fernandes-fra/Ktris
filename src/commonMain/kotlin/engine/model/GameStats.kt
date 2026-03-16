package engine.model

data class GameStats(
    var level: Int = 1,
    var totalLinesCleared: Int = 0,
    var combo: Int = -1,
    var b2bCount: Int = -1,
) : Resetable {
    override fun reset() {
        level = 1
        totalLinesCleared = 0
        combo = -1
        b2bCount = -1
    }
}