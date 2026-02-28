package util

import model.Matrix

object CollisionUtils {
    fun <T> checkCollision(
        container: Matrix<T>,
        candidate: Matrix<T>,
        startRow: Int,
        startCol: Int,
        emptyValue: T?
    ): Boolean {
        for (row in 0 until candidate.rows) {
            for (col in 0 until candidate.cols) {
                val pieceCell = candidate[row, col]
                if (pieceCell == null || pieceCell == emptyValue) continue

                val targetRow = startRow + row
                val targetCol = startCol + col

                val isOutOfBounds = targetRow !in 0 until container.rows ||
                        targetCol !in 0 until container.cols
                if (isOutOfBounds) return true

                val cellValue = container[targetRow, targetCol]
                val isOccupied = (cellValue != null && cellValue != emptyValue)

                if (isOccupied) {
                    return true
                }
            }
        }
        return false
    }

}