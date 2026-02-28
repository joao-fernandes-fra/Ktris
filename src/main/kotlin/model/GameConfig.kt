package model

enum class GameGoal {
    LINES, TIME, NONE
}

data class GameConfig(
    // --- Board Dimensions ---
    val boardRows: Int = 20,
    val boardCols: Int = 10,
    val hiddenRows: Int = 2,

    // --- Handling (The "Feel") ---
    val dasDelay: Float = 120f,
    val arrDelay: Float = 0f,
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
    val isTSpinEnabled: Boolean = true,
    val previewSize: Int = 5,
    val bagMultiplier: Int = 1,

    // --- Win Conditions ---
    val goalType: GameGoal = GameGoal.NONE,
    val goalValue: Int = 0,
)