package model


interface BagRandomizer {
    fun getNextPiece(): Tetromino
    fun getPreview(count: Int): List<Tetromino>
}

class MultiBagRandomizer(
    private val setsPerBag: Int = 1, // 1 = 7-bag, 2 = 14-bag, 3 = 21-bag
    private val previewSize: Int = 5
) : BagRandomizer {

    private val queue: MutableList<Tetromino> = mutableListOf()

    init {
        refill()
    }

    override fun getNextPiece(): Tetromino {
        if (queue.size <= previewSize) {
            refill()
        }
        return queue.removeAt(0)
    }

    override fun getPreview(count: Int): List<Tetromino> {
        return queue.take(count)
    }

    private fun refill() {
        val newBag = mutableListOf<Tetromino>()
        // Generate 'setsPerBag' full sets of Tetrominoes (7 pieces each)
        repeat(setsPerBag) {
            newBag.addAll(Tetromino.entries)
        }
        newBag.shuffle()
        queue.addAll(newBag)
    }
}