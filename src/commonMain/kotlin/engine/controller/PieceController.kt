package engine.controller

import engine.model.LastPieceAction
import engine.model.MovingPiece
import engine.model.Piece
import engine.model.Resetable
import engine.model.Rotation

interface PieceController : Resetable {
    var currentPiece: MovingPiece?
    var lastAction: LastPieceAction
    fun getNextPieces(previewSize: Int = 1): List<Piece>
    fun spawn(piece: Piece? = null): MovingPiece?
    fun move(targetRow: Int, targetCol: Int): Boolean
    fun rotate(rotation: Rotation): Boolean
    fun clearPiece()
}

interface GravityCapable {
    fun handleGravity(delta: Double, gravitySpeed: Double)
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
    val lockResetsRemaining: Int
    fun handleLockDelay(deltaTime: Double, onLock: () -> Unit): Boolean
}

interface HoldCapable {
    var heldPiece: Piece?
    var holdBuffered: Boolean
    fun hold()
}

interface GhostCapable {
    var ghostRow: Int
    fun updateGhost()
}

interface SoftDropCapable {
    fun softDrop(deltaTime: Double, gravitySpeed: Double)
}

interface HardDropCapable {
    fun hardDrop()
}

interface InitialActionsCapable {
    fun bufferRotation(rotation: Rotation, isFreshPress: Boolean = true)
    fun bufferHold(isFreshPress: Boolean = true)
    fun clearActionBuffer()
}

interface InputBufferCapable {
    val rotationBufferWindow: Double
    fun tickInputBuffer(delta: Double)
}

interface SpinTrackingCapable {
    val lastKickIndex: Int
}
