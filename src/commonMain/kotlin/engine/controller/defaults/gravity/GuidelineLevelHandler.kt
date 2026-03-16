package engine.controller.defaults.gravity

import engine.controller.LevelHandler

class GuidelineLevelHandler(
    override val startingLevel: Int = 1,
    private val levelCap: Int = 15,
) : LevelHandler {

    // Lines required to reach each level — guideline step table
    private val levelThresholds = listOf(
        0, 10, 20, 30, 40, 50, 60, 70, 80, 90,   // 1–10
        100, 120, 140, 160, 180, 200             // 11–15+
    )

    // Guideline gravity in ms per row — (level → ms)
    private val gravityTable = mapOf(
        1 to 1000.0,  2 to 793.0,  3 to 618.0,
        4 to 473.0,   5 to 355.0,  6 to 262.0,
        7 to 190.0,   8 to 135.0,  9 to 94.0,
        10 to 64.0,  11 to 43.0,  12 to 28.0,
        13 to 18.0,  14 to 11.0,  15 to 7.0,
    )

    override fun levelForLines(linesCleared: Int, currentLevel: Int): Int {
        val newLevel = levelThresholds.indexOfLast { linesCleared >= it } + 1
        return newLevel.coerceIn(currentLevel, levelCap)
    }

    override fun gravitySpeed(level: Int): Double {
        return gravityTable[level.coerceAtMost(levelCap)] ?: 7.0
    }
}