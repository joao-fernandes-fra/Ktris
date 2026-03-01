package controller

import model.Matrix
import model.defaults.DefaultMovingPiece

interface BoardController {
    val board: Matrix<Int>
    var linesCleared: Int
    val isBoardEmpty: Boolean
    fun isOccupied(row: Int, col: Int): Boolean
    fun clearFullLines(): Int
    fun getFullLines(): List<Int>
    fun addGarbage(lines: Int, garbageBlockId: Int)
    fun placePiece(piece: DefaultMovingPiece<*>)
}