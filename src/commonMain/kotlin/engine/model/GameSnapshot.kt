package engine.model

data class GameSnapshot<T : Piece>(
    val board: Board,
    val currentPiece: PieceState<T>?,
    val ghostPiece: PieceState<T>?,
    val nextPieces: List<T?>,
    val holdPiece: T?,
    val hudInfo: HUDInfo,
    val timeState: TimeState,
    val timeStateProgress: Double?,
    val gameState: GameState,
    val pendingClearLines: Collection<Int>,
)

data class HUDInfo(
    val combo: Int?,
    val b2bCount: Int?,
    val lockResetsRemaining: Int?
)

data class PieceState<T : Piece>(
    val shape: Matrix,
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