package model

enum class GameGoal {
    LINES, TIME, NONE
}

enum class TimeMode { NORMAL, FROZEN, SLOWED }

data class GameConfig(
    // --- Board Dimensions ---
    val boardRows: Int = 20,
    val boardCols: Int = 10,

    // --- Handling ("The Feel") ---
    val dasDelay: Float = 166f,
    val arrDelay: Float = 33f,
    val entryDelay: Float = 500f,
    val lockDelay: Float = 500f,
    val softDropDelay: Float = 50f,
    val maxLockResets: Int = 15,

    // --- Gravity & Difficulty ---
    val gravityBase: Float = 1000f,
    val gravityIncrement: Float = 50f,
    val levelCap: Int = 99,

    // --- Mechanics ---
    val isHoldEnabled: Boolean = true,
    val isGhostEnabled: Boolean = true,
    val isSpinEnabled: Boolean = true,
    val is180Enabled: Boolean = false,  // 180 rotation logic is not quite ready, currently the app just handle math rotated pieces using the Matrix and breaks the game, it is disabled by default
    val previewSize: Int = 5,
    val bagMultiplier: Int = 1,
    val slowDownMultiplier: Float = 1f,

    // --- Win Conditions ---
    val goalType: GameGoal = GameGoal.NONE,
    val goalValue: Int = 0,
)