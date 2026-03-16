package engine.controller.defaults.gravity

import engine.controller.LevelHandler

class LinearLevelHandler(
    override val startingLevel: Int = 1,
    private val gravityBase: Double = 1000.0,
    private val gravityIncrement: Double = 0.8,
    private val levelCap: Int = 99,
    private val linesPerLevel: Int = 10,
) : LevelHandler {

    override fun levelForLines(linesCleared: Int, currentLevel: Int): Int {
        val newLevel = (linesCleared / linesPerLevel) + startingLevel
        return newLevel.coerceAtMost(levelCap)
    }

    override fun gravitySpeed(level: Int): Double {
        return (gravityBase - (level - 1) * gravityIncrement).coerceAtLeast(0.0)
    }
}