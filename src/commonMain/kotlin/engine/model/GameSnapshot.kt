package engine.model

data class GameSnapshot(
    val board: Board,
    val currentPiece: PieceState?,
    val ghostPiece: PieceState?,
    val nextPieces: List<Piece?>,
    val holdPiece: Piece?,
    val hudInfo: HUDInfo,
    val timeState: TimeState,
    val timeStateProgress: Double?,
    val gameState: GameState,
    val pendingClearLines: Collection<Int>,
)

data class HUDInfo(
    val totalLinesCleared: Int?,
    val currentLevel: Int?,
    val combo: Int?,
    val b2bCount: Int?,
    val lockResetsRemaining: Int?,
    val sessionTimeSeconds: Double?,
)

data class PieceState(
    val shape: Matrix,
    val row: Int,
    val col: Int,
    val type: Piece
)

fun Piece.toPieceState(): PieceState {
    return PieceState(
        shape,
        0,
        0,
        this
    )
}