package engine.controller.defaults

import engine.controller.BoardController
import engine.model.Board
import engine.model.Board.Companion.EMPTY_BLOCK_VALUE
import engine.model.Matrix
import engine.model.MovingPiece


class BoardManager(rows: Int, cols: Int, bufferHeight: Int) : BoardController {
    companion object {
        private const val PENDING_BLOCK_ID = -1
    }

    override val board: Board = Board(rows, cols, bufferHeight, EMPTY_BLOCK_VALUE)
    override var linesCleared: Int = 0
    override val isBoardEmpty get() = board.isEmpty

    override fun isOccupied(row: Int, col: Int): Boolean = board.isOccupied(row, col)

    override fun clearFullLines(): Set<Int> {
        val fullLines = getFullLines()

        fullLines.forEach { row ->
            clearRow(row)
            linesCleared++
        }
        return fullLines
    }

    override fun getFullLines(): Set<Int> {
        return (0 until board.rows).filter { row -> isRowFull(row) }.toSet()
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

    override fun collapseFullLines() {
        val fullLines = getFullLines()
        fullLines.forEach {
            clearRow(it)
        }

        fullLines.forEach {
            shiftBoardUp()
            for (c in 0 until board.cols) {
                board[board.rows - 1, c] = PENDING_BLOCK_ID
            }
        }
    }

    override fun updateBoard(board: Board) {
        if (board == this.board) {
            this.board.contents = board.contents
        }
    }

    override fun addGarbage(lines: Int, garbageBlockId: Int) {
        if (lines <= 0) return

        val holeCol = (0 until board.cols).random()

        repeat(lines) {
            shiftBoardUp()
            for (c in 0 until board.cols) {
                board[board.rows - 1, c] = if (c == holeCol) EMPTY_BLOCK_VALUE else garbageBlockId
            }
        }
    }

    private fun shiftBoardUp() {
        for (r in 0 until board.rows - 1) {
            for (c in 0 until board.cols) {
                board[r, c] = board[r + 1, c]
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

    override fun reset() {
        board.contents = Matrix(board.rows, board.cols, EMPTY_BLOCK_VALUE)
        linesCleared = 0
    }
}