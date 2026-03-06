package engine.model

enum class GameGoal {
    LINES, TIME, NONE
}

data class GlobalGameSettings(
    val boardRows: Int = 20,
    val boardCols: Int = 10,
    val bufferZone: Int = 4,

    val gravityBase: Double = 1000.0,
    val gravityIncrement: Double = 0.8,
    val levelCap: Int = 99,

    val shouldCollapseOnFreeze: Boolean = true,
    val bagMultiplier: Int = 1,
    val slowDownMultiplier: Double = 2.0,

    val goalType: GameGoal = GameGoal.NONE,
    val goalValue: Double = 0.0,
    val garbageBlockId: Int = -99
)

data class PlayerSettings(
    // Handling ("in millis")
    val dasDelay: Double = 167.0,
    val arrDelay: Double = 33.0,
    val entryDelay: Double = 200.0,
    val lockDelay: Double = 500.0,
    val softDropDelay: Double = 33.0,
    val maxLockResets: Int = 15,

    val isHoldEnabled: Boolean = true,
    val isGhostEnabled: Boolean = true,
    val isSpinEnabled: Boolean = true,
    val is180Enabled: Boolean = false,

    val previewSize: Int = 5,
)
