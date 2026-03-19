package engine.controller

import engine.model.Piece
import engine.model.Resetable

interface PieceRandomizer : Resetable {
    fun getNextPiece(): Piece
    fun getPreview(count: Int): List<Piece>
}