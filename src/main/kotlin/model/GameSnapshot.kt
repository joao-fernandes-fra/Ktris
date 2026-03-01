package model

data class GameSnapshot<T : Piece>(
    val board: Matrix<Int>,
    val currentPiece: PieceState<T>?,
    val ghostPiece: PieceState<T>?,
    val nextPieces: List<T?>,
    val holdPiece: T?
)

data class PieceState<T : Piece>(
    val shape: Matrix<Int>,
    val row: Int,
    val col: Int,
    val type: T
)

fun <T : Piece> T.toPieceState(): PieceState<T> {
    return PieceState(
        shape,
        0,
        0,
        this
    )
}