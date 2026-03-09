package engine.controller.defaults

import engine.controller.BagRandomizer
import engine.model.Piece

class SevenBagRandomizer<T : Piece>(
    private val availablePieces: Collection<T>,
    private val previewSize: Int = 5
) : BagRandomizer<T> {

    private val queue: MutableList<T> = mutableListOf()
    private val currentBag: MutableList<T> = mutableListOf()

    init {
        refill()
    }

    override fun getNextPiece(): T {
        if (queue.isEmpty()) {
            refill()
        }
        val piece = queue.removeAt(0)

        refill()
        return piece
    }

    override fun getPreview(count: Int): List<T> {
        if (queue.size < count) {
            refill()
        }
        return try {
            queue.toList().take(count)
        } catch (_: Exception) {
            return emptyList()
        }
    }


    private fun refill() {
        while (queue.size <= previewSize) {
            if (currentBag.isEmpty()) {
                currentBag.addAll(availablePieces.shuffled())
            }
            if (currentBag.isNotEmpty()) {
                queue.add(currentBag.removeAt(0))
            } else {
                break
            }
        }
    }


    override fun reset() {
        queue.clear()
        currentBag.clear()
        refill()
    }
}