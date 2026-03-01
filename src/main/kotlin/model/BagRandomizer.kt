package model


interface BagRandomizer<T : Piece> {
    fun getNextPiece(): T
    fun getPreview(count: Int): List<T>
}