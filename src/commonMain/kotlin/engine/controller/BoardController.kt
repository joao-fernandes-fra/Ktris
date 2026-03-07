package engine.controller

import engine.model.Board
import engine.model.MovingPiece
import engine.model.Resetable

interface BoardController : Resetable {
    val board: Board
    var linesCleared: Int
    val isBoardEmpty: Boolean
    fun isOccupied(row: Int, col: Int): Boolean
    fun clearFullLines(): Set<Int>
    fun getFullLines(): Set<Int>
    fun placePiece(piece: MovingPiece<*>)
    fun updateBoard(board: Board)
}

interface CollapseCapable {
    fun collapseFullLines()
}

interface GarbageCapable {
    fun addGarbage(lines: Int, garbageBlockId: Int)
}