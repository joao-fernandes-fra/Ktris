package engine.util

import engine.model.Board
import engine.model.Matrix
import engine.model.MovingPiece

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

    fun isImmobile(board: Board, piece: MovingPiece<*>): Boolean {
        val shape = piece.shape
        val row = piece.pieceRow
        val col = piece.pieceCol
        return listOf(0 to -1, 0 to 1, 1 to 0, -1 to 0).all { (dr, dc) ->
            checkCollisionWithBoard(board, shape, row + dr, col + dc)
        }
    }
}