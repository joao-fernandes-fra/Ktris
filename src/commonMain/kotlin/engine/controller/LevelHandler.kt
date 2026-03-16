package engine.controller

interface LevelHandler {
    val startingLevel: Int
    fun levelForLines(linesCleared: Int, currentLevel: Int): Int
    fun gravitySpeed(level: Int): Double
}