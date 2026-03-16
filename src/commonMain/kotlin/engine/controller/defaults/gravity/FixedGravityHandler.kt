package engine.controller.defaults.gravity

import engine.controller.LevelHandler

class FixedGravityHandler(
    override val startingLevel: Int = 1,
    private val fixedSpeed: Double = 1000.0,
    private val linesPerLevel: Int = 10,
    private val levelCap: Int = 99,
) : LevelHandler {

    override fun levelForLines(linesCleared: Int, currentLevel: Int): Int {
        return ((linesCleared / linesPerLevel) + startingLevel).coerceAtMost(levelCap)
    }

    override fun gravitySpeed(level: Int): Double = fixedSpeed
}