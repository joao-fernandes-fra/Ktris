package model.defaults

import model.BagRandomizer
import model.Piece

class MultiBagRandomizer<T : Piece>(
    private val availablePieces: List<T>,
    private val setsPerBag: Int = 1,
    private val previewSize: Int = 5
) : BagRandomizer<T> {

    private val queue: MutableList<T> = mutableListOf()

    init {
        refill()
    }

    override fun getNextPiece(): T {
        if (queue.size <= previewSize) {
            refill()
        }
        return queue.removeAt(0)
    }

    override fun getPreview(count: Int): List<T> {
        return queue.take(count)
    }

    private fun refill() {
        val newBag = mutableListOf<T>()
        repeat(setsPerBag) {
            newBag.addAll(availablePieces)
        }
        newBag.shuffle()
        queue.addAll(newBag)
    }
}