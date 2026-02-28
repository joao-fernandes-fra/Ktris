package util

import model.Matrix

enum class TSpinType { NONE, MINI, REGULAR }

object TSpinDetector {
    fun checkTSpin(
        board: Matrix<Int>,
        centerRow: Int,
        centerCol: Int,
        rotation: Int
    ): TSpinType {
        // Define the 4 corners: 0:TopLeft, 1:TopRight, 2:BottomLeft, 3:BottomRight
        val corners = listOf(
            centerRow - 1 to centerCol - 1,
            centerRow - 1 to centerCol + 1,
            centerRow + 1 to centerCol - 1,
            centerRow + 1 to centerCol + 1
        )

        val occupied = corners.map { (r, c) -> board.isOccupied(r, c) }
        val occupiedCount = occupied.count { it }

        if (occupiedCount < 3) return TSpinType.NONE

        // Determine which corners are the "Front" (facing direction of the T-tip)
        // 0: Up, 1: Right, 2: Down, 3: Left
        val frontCorners = when (rotation) {
            0 -> listOf(0, 1) // Pointing UP (Top corners are front)
            1 -> listOf(1, 3) // Pointing RIGHT (Right corners are front)
            2 -> listOf(2, 3) // Pointing DOWN (Bottom corners are front)
            3 -> listOf(0, 2) // Pointing LEFT (Left corners are front)
            else -> listOf()
        }

        // A regular T-Spin must have both front corners blocked.
        // 4 corners filled is ALWAYS a Regular T-Spin.
        val frontBlocked = frontCorners.all { occupied[it] }

        return if (occupiedCount == 4 || frontBlocked) {
            TSpinType.REGULAR
        } else {
            TSpinType.MINI
        }
    }

    private fun Matrix<Int>.isOccupied(row: Int, col: Int, emptyValue: Int = 0): Boolean {
        if (row !in 0..<rows || col < 0 || col >= cols) return true
        return this[row, col] != emptyValue
    }
}