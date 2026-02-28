package model

data class GameSnapshot(
    val board: Matrix<Int>,
    val currentPiece: PieceState?,
    val ghostPiece: PieceState?,
    val nextPieces: List<Tetromino?>,
    val holdPiece: Tetromino?
)

data class PieceState(
    val shape: Matrix<Int>,
    val row: Int,
    val col: Int,
    val type: Tetromino
)

fun Tetromino.toPieceState(): PieceState {
    return PieceState(
        shape,
        0,
        0,
        this
    )
}