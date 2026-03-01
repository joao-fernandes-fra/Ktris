package util

import model.Matrix

object CollisionUtils {
    fun checkCollision(
        container: Matrix<Int>,
        candidate: Matrix<Int>,
        startRow: Int,
        startCol: Int,
    ): Boolean {
        for (row in 0 until candidate.rows) {
            for (col in 0 until candidate.cols) {
                val pieceCell = candidate[row, col]
                if (pieceCell == null || pieceCell == 0) continue

                val targetRow = startRow + row
                val targetCol = startCol + col

                if (targetCol !in 0 until container.cols) return true

                if (targetRow >= container.rows) return true

                if (targetRow >= 0) {
                    val cellValue = container[targetRow, targetCol]
                    if (cellValue != null && cellValue != 0) return true
                }
            }
        }
        return false
    }

}