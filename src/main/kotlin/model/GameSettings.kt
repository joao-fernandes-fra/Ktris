package model

enum class GameGoal {
    LINES, TIME, NONE
}

data class GameSettings(
    // --- Board Dimensions ---
    val boardRows: Int = 20,
    val boardCols: Int = 10,
    val bufferHeight: Int = 4,

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
    val shouldCollapseOnFreeze: Boolean = true, // on freeze mode define whether it should collapse the lines to the bottom or keep them in place
    // 180 rotation logic is not quite ready, currently the app just handle procedural rotated pieces using the Matrix and this breaks the game with 180 rotations active, it is disabled by default but u can enable it to try
    val is180Enabled: Boolean = false,
    val previewSize: Int = 5,
    val bagMultiplier: Int = 1,
    val slowDownMultiplier: Float = 1f,

    // --- Win Conditions ---
    val goalType: GameGoal = GameGoal.NONE,
    val goalValue: Int = 0,
)