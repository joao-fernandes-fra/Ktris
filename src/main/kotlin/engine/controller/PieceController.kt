package engine.controller

import engine.model.LastPieceAction
import engine.model.MovingPiece
import engine.model.Piece
import engine.model.Resetable
import engine.model.Rotation

interface PieceController<T : Piece> : Resetable {
    var heldPiece: T?
    var currentPiece: MovingPiece<T>?
    var ghostRow: Int
    var lastAction: LastPieceAction
    suspend fun handleDAS(delta: Float, currentDirection: Int?)
    suspend fun resetDas()
    suspend fun handleGravity(currentLevel: Int, delta: Float)
    suspend fun spawn(piece: T): MovingPiece<T>?
    suspend fun hardDrop()
    suspend fun softDrop(deltaTime: Float)
    suspend fun move(targetRow: Int, targetCol: Int): Boolean
    suspend fun rotate(rotation: Rotation): Boolean
    suspend fun holdPiece(getNextPiece: () -> T)
    suspend fun clearPiece()
    suspend fun handleLockDelay(deltaTime: Float, onLock: suspend () -> Unit)
}