package engine.controller.defaults.board

import engine.controller.CollapseCapable
import engine.controller.GarbageCapable
import engine.model.Board
import engine.model.GarbageConfig

class ExtendedBoardController(
    rows: Int, cols: Int, bufferHeight: Int,
    private val garbageConfig: GarbageConfig
) :
    DefaultBoardController(rows, cols, bufferHeight), CollapseCapable, GarbageCapable {
    companion object {
        private const val PENDING_BLOCK_ID = -1
    }

    override fun collapseFullLines() {
        val fullLines = getFullLines()
        fullLines.forEach {
            clearRow(it)
        }

        fullLines.forEach { _ ->
            shiftBoardUp()
            for (c in 0 until board.cols) {
                board[board.rows - 1, c] = PENDING_BLOCK_ID
            }
        }
    }

    override fun addGarbage(lines: Int, garbageBlockId: Int) {
        if (lines <= 0) return

        val messiness = garbageConfig.messiness
        var holeCol = (0 until board.cols).random()

        repeat(lines) {
            if ((0..99).random() < messiness) {
                holeCol = (0 until board.cols).random()
            }

            shiftBoardUp()
            for (c in 0 until board.cols) {
                board[board.rows - 1, c] = if (c == holeCol) Board.EMPTY_BLOCK_VALUE else garbageBlockId
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