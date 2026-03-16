package engine.controller.defaults.piece

import engine.controller.BagRandomizer
import engine.model.Piece

class SevenBagRandomizer<T : Piece>(
    private val availablePieces: Collection<T>
) : BagRandomizer<T> {

    private val queue: MutableList<T> = mutableListOf()
    private val currentBag: MutableList<T> = mutableListOf()

    init {
        refill()
    }

    override fun getNextPiece(): T {
        if (queue.isEmpty()) refill()
        val piece = queue.removeAt(0)
        refill()
        return piece
    }

    override fun getPreview(count: Int): List<T> {
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