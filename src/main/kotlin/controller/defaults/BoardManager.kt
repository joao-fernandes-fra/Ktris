package controller.defaults

import controller.BoardController
import model.Matrix
import model.MovingPiece
import model.defaults.DefaultMovingPiece


class BoardManager(rows: Int, cols: Int) : BoardController {
    companion object {
        private const val EMPTY_BLOCK_VALUE = 0
    }

    override val board = Matrix(rows, cols, EMPTY_BLOCK_VALUE)
    override var linesCleared: Int = 0
    override val isBoardEmpty = board.isEmpty(EMPTY_BLOCK_VALUE)

    override fun isOccupied(row: Int, col: Int): Boolean = board[row, col] != EMPTY_BLOCK_VALUE

    override fun clearFullLines(): Int {
        val fullLines = getFullLines()

        fullLines.forEach { row ->
            clearRow(row)
            linesCleared++
        }

        return fullLines.size
    }

    override fun getFullLines(): List<Int> {
        return (0 until board.rows).filter { row -> isRowFull(row) }
    }

    private fun isRowFull(row: Int): Boolean {
        return (0 until board.cols).all { col -> isOccupied(row, col) }
    }

    private fun clearRow(rowToClear: Int) {
        for (r in rowToClear downTo 1) {
            for (c in 0 until board.cols) {
                board[r, c] = board[r - 1, c]
            }
        }
        for (c in 0 until board.cols) {
            board[0, c] = EMPTY_BLOCK_VALUE
        }
    }

    override fun addGarbage(lines: Int, garbageBlockId: Int) {
        if (lines <= 0) return

        val holeCol = (0 until board.cols).random()

        repeat(lines) {
            for (r in 0 until board.rows - 1) {
                for (c in 0 until board.cols) {
                    board[r, c] = board[r + 1, c]
                }
            }
            for (c in 0 until board.cols) {
                board[board.rows - 1, c] = if (c == holeCol) EMPTY_BLOCK_VALUE else garbageBlockId
            }
        }
    }

    override fun placePiece(piece: MovingPiece<*>) {
        val shape = piece.shape
        for (r in 0 until shape.rows) {
            for (c in 0 until shape.cols) {
                if (shape[r, c] != EMPTY_BLOCK_VALUE) {
                    board[piece.pieceRow + r, piece.pieceCol + c] = shape[r, c]
                }
            }
        }
    }
}