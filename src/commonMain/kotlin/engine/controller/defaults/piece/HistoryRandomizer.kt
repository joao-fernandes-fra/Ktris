package engine.controller.defaults.piece

import engine.controller.BagRandomizer
import engine.model.Piece

class HistoryRandomizer<T : Piece>(
    private val availablePieces: Collection<T>,
    private val historySize: Int = 4
) : BagRandomizer<T> {

    private val history: ArrayDeque<T> = ArrayDeque()
    private val preview: MutableList<T> = mutableListOf()

    init {
        refillPreview(1)
    }

    override fun getNextPiece(): T {
        if (preview.isEmpty()) refillPreview(1)
        val piece = preview.removeAt(0)
        history.addLast(piece)
        if (history.size > historySize) history.removeFirst()
        refillPreview(1)
        return piece
    }

    override fun getPreview(count: Int): List<T> {
        refillPreview(count)
        return preview.toList().take(count)
    }

    private fun refillPreview(upTo: Int) {
        while (preview.size < upTo) {
            val pool = availablePieces.filter { it !in history }
                .ifEmpty { availablePieces.toList() }
            preview.add(pool.random())
        }
    }

    override fun reset() {
        history.clear()
        preview.clear()
        refillPreview(1)
    }
}