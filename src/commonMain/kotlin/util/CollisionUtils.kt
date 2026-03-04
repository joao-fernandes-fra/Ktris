package util

import model.Board
import model.Matrix

object CollisionUtils {
    fun checkCollisionWithBoard(
        board: Board,
        piece: Matrix,
        startRow: Int,
        startCol: Int,
    ): Boolean {
        for (row in 0 until piece.rows) {
            for (col in 0 until piece.cols) {
                val pieceCell = piece[row, col]
                if (pieceCell == 0) continue

                val targetRow = startRow + row
                val targetCol = startCol + col

                if (targetCol !in 0 until board.cols) return true

                if (targetRow >= board.rows) return true

                if (targetRow < -board.bufferSize) return true

                if (targetRow < 0) continue

                val cellValue = board[targetRow, targetCol]
                if (cellValue != 0) return true
            }
        }
        return false
    }
}