package engine.controller.defaults.piece

import engine.controller.PieceRandomizer
import engine.model.Piece

class ClassicRandomizer(
    private val availablePieces: Collection<Piece>
) : PieceRandomizer {

    private val preview: MutableList<Piece> = mutableListOf()

    init {
        refillPreview(1)
    }

    override fun getNextPiece(): Piece {
        if (preview.isEmpty()) refillPreview(1)
        val piece = preview.removeAt(0)
        refillPreview(1)
        return piece
    }

    override fun getPreview(count: Int): List<Piece> {
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