package engine.controller

import engine.model.Piece
import engine.model.Resetable

interface BagRandomizer<T : Piece> : Resetable {
    fun getNextPiece(): T
    fun getPreview(count: Int): List<T>
}