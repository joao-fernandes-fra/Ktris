package engine.controller.defaults

import engine.controller.BagRandomizer
import engine.model.Piece

class ClassicPieceRandomizer<T : Piece>(private val availablePieces: Collection<T>) : BagRandomizer<T> {

    private var nextPiece: T

    init {
        nextPiece = availablePieces.random()
    }

    override fun getNextPiece(): T {
        return nextPiece.apply {
            nextPiece = availablePieces.random()
        }
    }

    override fun getPreview(count: Int): List<T> {
        return listOfNotNull(nextPiece)
    }

    override fun reset() {
        nextPiece = availablePieces.random()
    }
}