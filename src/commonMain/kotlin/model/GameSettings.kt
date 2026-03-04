package model

enum class GameGoal {
    LINES, TIME, NONE
}

data class GameSettings(
    val boardRows: Int = 20,
    val boardCols: Int = 10,
    val bufferHeight: Int = 4,

    // --- Handling ("in millis") ---
    val dasDelay: Float = 167f,      // ~10 frames
    val arrDelay: Float = 33f,       // ~2 frames
    val entryDelay: Float = 200f,     // Standard ARE
    val lockDelay: Float = 500f,      // Standard "wiggle" time
    val softDropDelay: Float = 33f,   // Fast
    val maxLockResets: Int = 15,

    val gravityBase: Float = 1000f,   // 1 second per drop
    val gravityIncrement: Float = 0.8f, // Multiplier (classic) or fixed decrement
    val levelCap: Int = 99,

    val isHoldEnabled: Boolean = true,
    val isGhostEnabled: Boolean = true,
    val isSpinEnabled: Boolean = true,
    val shouldCollapseOnFreeze: Boolean = true,
    val is180Enabled: Boolean = false,
    val previewSize: Int = 5,
    val bagMultiplier: Int = 1,
    val slowDownMultiplier: Float = 2f,

    val goalType: GameGoal = GameGoal.NONE,
    val goalValue: Float = 0f,
)