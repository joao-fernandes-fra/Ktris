package engine.controller.defaults

import engine.controller.CollapseCapable
import engine.controller.GarbageCapable
import engine.model.Board.Companion.EMPTY_BLOCK_VALUE


class GuidelineBoardController(rows: Int, cols: Int, bufferHeight: Int) :
    DefaultBoardController(rows, cols, bufferHeight), CollapseCapable, GarbageCapable {
    companion object {
        private const val PENDING_BLOCK_ID = -1
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
}