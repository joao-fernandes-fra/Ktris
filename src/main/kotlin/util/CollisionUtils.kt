package util

import model.Board
import model.Matrix

object CollisionUtils {
    fun checkCollisionWithBoard(
        board: Board,
        piece: Matrix<Int>,
        startRow: Int,
        startCol: Int,
    ): Boolean {
        for (row in 0 until piece.rows) {
            for (col in 0 until piece.cols) {
                val pieceCell = piece[row, col]
                if (pieceCell == null || pieceCell == 0) continue

                val targetRow = startRow + row
                val targetCol = startCol + col

                if (targetCol !in 0 until board.cols) return true

                if (targetRow >= board.rows) return true

                if (targetRow < -board.bufferHeight) return true

                val cellValue = board[targetRow, targetCol]
                if (cellValue != null && cellValue != 0) return true
            }
        }
        return false
    }
}