package engine.controller

import engine.model.LastPieceAction
import engine.model.MovingPiece
import engine.model.Piece
import engine.model.Resetable
import engine.model.Rotation

interface PieceController<T : Piece> : Resetable {
    var currentPiece: MovingPiece<T>?
    var lastAction: LastPieceAction
    suspend fun getNextPieces(previewSize: Int = 1): List<T>
    suspend fun spawn(piece: T? = null): MovingPiece<T>?
    suspend fun move(targetRow: Int, targetCol: Int): Boolean
    suspend fun rotate(rotation: Rotation): Boolean
    suspend fun clearPiece()
}

interface GravityCapable {
    suspend fun handleGravity(delta: Double)
}

interface DasCapable {
    suspend fun handleDAS(delta: Double, currentDirection: Int?)
    suspend fun resetDas()
}

interface ClipCapable {
    suspend fun clip()
}

interface LockDelayCapable {
    suspend fun handleLockDelay(deltaTime: Double, onLock: suspend () -> Unit): Boolean
}

interface HoldCapable<T : Piece> {
    var heldPiece: T?
    suspend fun hold()
}

interface GhostCapable {
    var ghostRow: Int
    suspend fun updateGhost()
}

interface SoftDropCapable {
    suspend fun softDrop(deltaTime: Double)
}

interface HardDropCapable {
    suspend fun hardDrop()
}
