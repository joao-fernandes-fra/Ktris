package engine.controller

import engine.model.LastPieceAction
import engine.model.MovingPiece
import engine.model.Piece
import engine.model.Resetable
import engine.model.Rotation

interface PieceController<T : Piece> : Resetable {
    var currentPiece: MovingPiece<T>?
    var lastAction: LastPieceAction
    fun getNextPieces(previewSize: Int = 1): List<T>
    fun spawn(piece: T? = null): MovingPiece<T>?
    fun move(targetRow: Int, targetCol: Int): Boolean
    fun rotate(rotation: Rotation): Boolean
    fun clearPiece()
}

interface GravityCapable {
    fun handleGravity(delta: Double)
}

interface DasCapable {
    fun handleDAS(delta: Double, currentDirection: Int?)
    fun resetDas()
    fun preserveDas()
}

interface ClipCapable {
    fun clip()
}

interface LockDelayCapable {
    fun handleLockDelay(deltaTime: Double, onLock: () -> Unit): Boolean
}

interface HoldCapable<T : Piece> {
    var heldPiece: T?
    var holdBuffered: Boolean
    fun hold()
}

interface GhostCapable {
    var ghostRow: Int
    fun updateGhost()
}

interface SoftDropCapable {
    fun softDrop(deltaTime: Double)
}

interface HardDropCapable {
    fun hardDrop()
}

interface InitialActionsCapable {
    fun bufferRotation(rotation: Rotation)
    fun bufferHold()
    fun clearActionBuffer()
}

interface InputBufferCapable {
    val rotationBufferWindow: Double
    fun tickInputBuffer(delta: Double)
}

interface SpinTrackingCapable {
    val lastKickIndex: Int
}
