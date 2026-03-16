package engine.controller.defaults.piece

import engine.controller.BagRandomizer
import engine.model.Piece

class ClassicRandomizer<T : Piece>(
    private val availablePieces: Collection<T>
) : BagRandomizer<T> {

    private val preview: MutableList<T> = mutableListOf()

    init { refillPreview(1) }

    override fun getNextPiece(): T {
        if (preview.isEmpty()) refillPreview(1)
        val piece = preview.removeAt(0)
        refillPreview(1)
        return piece
    }

    override fun getPreview(count: Int): List<T> {
        refillPreview(count)
        return preview.toList().take(count)
    }

    private fun refillPreview(upTo: Int) {
        while (preview.size < upTo) preview.add(availablePieces.random())
    }

    override fun reset() {
        preview.clear()
        refillPreview(1)
    }
}