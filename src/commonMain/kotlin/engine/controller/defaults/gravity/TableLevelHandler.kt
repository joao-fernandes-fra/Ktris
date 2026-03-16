package engine.controller.defaults.gravity

import engine.controller.LevelHandler

class TableLevelHandler(
    override val startingLevel: Int = 1,
    private val lineThresholds: List<Int>,
    private val gravitySpeeds: List<Double>,
) : LevelHandler {

    override fun levelForLines(linesCleared: Int, currentLevel: Int): Int {
        val newLevel = lineThresholds.indexOfLast { linesCleared >= it } + 1
        return newLevel.coerceIn(currentLevel, lineThresholds.size)
    }

    override fun gravitySpeed(level: Int): Double {
        return gravitySpeeds.getOrElse(level - 1) { gravitySpeeds.last() }
    }
}