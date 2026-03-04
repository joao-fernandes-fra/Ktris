package controller

import model.Board
import model.MovingPiece
import model.Resetable

interface BoardController : Resetable {
    val board: Board
    var linesCleared: Int
    val isBoardEmpty: Boolean
    fun isOccupied(row: Int, col: Int): Boolean
    fun clearFullLines(): Int
    fun getFullLines(): List<Int>
    fun addGarbage(lines: Int, garbageBlockId: Int)
    fun placePiece(piece: MovingPiece<*>)
    fun collapseFullLines()
    fun updateBoard(board: Board)
}