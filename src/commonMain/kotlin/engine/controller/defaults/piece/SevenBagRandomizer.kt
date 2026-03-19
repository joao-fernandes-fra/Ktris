package engine.controller.defaults.piece

import engine.controller.PieceRandomizer
import engine.model.Piece

class SevenBagRandomizer(
    private val availablePieces: Collection<Piece>
) : PieceRandomizer {

    private val queue: MutableList<Piece> = mutableListOf()
    private val currentBag: MutableList<Piece> = mutableListOf()

    init {
        refill()
    }

    override fun getNextPiece(): Piece {
        if (queue.isEmpty()) refill()
        val piece = queue.removeAt(0)
        refill()
        return piece
    }

    override fun getPreview(count: Int): List<Piece> {
        while (queue.size < count) refill()
        return queue.toList().take(count)
    }

    private fun refill() {
        if (currentBag.isEmpty()) currentBag.addAll(availablePieces.shuffled())
        while (currentBag.isNotEmpty()) queue.add(currentBag.removeAt(0))
    }

    override fun reset() {
        queue.clear()
        currentBag.clear()
        refill()
    }
}