package engine.model

enum class GameGoal {
    LINES, TIME, NONE
}

data class GlobalGameSettings(
    val boardRows: Int = 20,
    val boardCols: Int = 10,
    val bufferZone: Int = 4,

    val gravityBase: Float = 1000f,
    val gravityIncrement: Float = 0.8f,
    val levelCap: Int = 99,

    val shouldCollapseOnFreeze: Boolean = true,
    val bagMultiplier: Int = 1,
    val slowDownMultiplier: Float = 2f,

    val goalType: GameGoal = GameGoal.NONE,
    val goalValue: Float = 0f,
    val garbageBlockId: Int = -99
)

data class PlayerSettings(
    // Handling ("in millis")
    val dasDelay: Float = 167f,
    val arrDelay: Float = 33f,
    val entryDelay: Float = 200f,
    val lockDelay: Float = 500f,
    val softDropDelay: Float = 33f,
    val maxLockResets: Int = 15,

    val isHoldEnabled: Boolean = true,
    val isGhostEnabled: Boolean = true,
    val isSpinEnabled: Boolean = true,
    val is180Enabled: Boolean = false,

    val previewSize: Int = 5,
)
