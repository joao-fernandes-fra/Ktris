package model


interface BagRandomizer<T : Piece> : Resetable {
    fun getNextPiece(): T
    fun getPreview(count: Int): List<T>
}