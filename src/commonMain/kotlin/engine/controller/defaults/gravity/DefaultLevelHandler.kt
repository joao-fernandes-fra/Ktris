package engine.controller.defaults.gravity

import engine.controller.LevelHandler
import kotlin.math.pow

class DefaultLevelHandler(
    override val startingLevel: Int = 1,
    private val levelCap: Int = 15,
) : LevelHandler {

    override fun gravitySpeed(level: Int): Double {
        val cappedLevel = level.coerceIn(1, levelCap)
        val base = 0.8 - (cappedLevel - 1) * 0.007
        val ms = 1000.0 * base.pow(cappedLevel - 1)
        return ms.coerceAtLeast(16.0)
    }

    private val levelThresholds = listOf(
        0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 180, 200
    )

    override fun levelForLines(linesCleared: Int, currentLevel: Int): Int {
        val newLevel = levelThresholds.indexOfLast { linesCleared >= it } + 1
        return newLevel.coerceIn(currentLevel, levelCap)
    }
}