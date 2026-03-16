package engine.controller.defaults.piece

import engine.controller.BagRandomizer
import engine.model.Piece

class FourteenBagRandomizer<T : Piece>(
    private val availablePieces: Collection<T>
) : BagRandomizer<T> {

    private val queue: MutableList<T> = mutableListOf()

    init {
        refill()
    }

    override fun getNextPiece(): T {
        if (queue.isEmpty()) refill()
        return queue.removeAt(0)
    }

    override fun getPreview(count: Int): List<T> {
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
