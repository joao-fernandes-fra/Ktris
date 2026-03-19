package engine.controller.defaults.piece

import engine.controller.PieceRandomizer
import engine.model.Piece

class FourteenBagRandomizer(
    private val availablePieces: Collection<Piece>
) : PieceRandomizer {

    private val queue: MutableList<Piece> = mutableListOf()

    init {
        refill()
    }

    override fun getNextPiece(): Piece {
        if (queue.isEmpty()) refill()
        return queue.removeAt(0)
    }

    override fun getPreview(count: Int): List<Piece> {
        while (queue.size < count) refill()
        return queue.toList().take(count)
    }

    private fun refill() {
        val doubleBag = (availablePieces + availablePieces).shuffled()
        queue.addAll(doubleBag)
    }

    override fun reset() {
        queue.clear()
        refill()
    }
}
