package engine.util

import engine.controller.BoardController
import engine.controller.ClipCapable
import engine.controller.CollapseCapable
import engine.controller.DasCapable
import engine.controller.GarbageCapable
import engine.controller.GhostCapable
import engine.controller.GravityCapable
import engine.controller.HardDropCapable
import engine.controller.HoldCapable
import engine.controller.LockDelayCapable
import engine.controller.PieceController
import engine.controller.SoftDropCapable
import engine.model.Piece

suspend fun <T : Piece> PieceController<T>?.handleDASIfSupported(delta: Double, currentDirection: Int?) {
    (this as? DasCapable)?.handleDAS(delta, currentDirection)
}

suspend fun <T : Piece> PieceController<T>?.applyGravityIfSupported(delta: Double) {
    (this as? GravityCapable)?.handleGravity(delta)
}

suspend fun <T : Piece> PieceController<T>?.clipIfSupported() {
    (this as? ClipCapable)?.clip()
}

suspend fun <T : Piece> PieceController<T>?.advanceLockIfSupported(delta: Double, onLock: suspend () -> Unit): Boolean {
    val lockCap = this as? LockDelayCapable
    if (lockCap != null) {
        return lockCap.handleLockDelay(delta, onLock)
    }
    return false
}

suspend fun <T : Piece> PieceController<T>?.updateGhostIfSupported() {
    (this as? GhostCapable)?.updateGhost()
}

fun <T : Piece> PieceController<T>?.getGhostRowIfSupported(): Int? {
    return (this as? GhostCapable)?.ghostRow
}

suspend fun <T : Piece> PieceController<T>?.holdIfSupported() {
    (this as? HoldCapable<T>)?.hold()
}

fun <T : Piece> PieceController<T>?.getHeldPieceIfSupported(): T? {
    return (this as? HoldCapable<T>)?.heldPiece
}

suspend fun <T : Piece> PieceController<T>?.hardDropIfSupported() {
    (this as? HardDropCapable)?.hardDrop()
}

suspend fun <T : Piece> PieceController<T>?.softDropIfSupported(deltaTime: Double) {
    (this as? SoftDropCapable)?.softDrop(deltaTime)
}

suspend fun <T : Piece> PieceController<T>?.resetDASifSupported() {
    (this as? DasCapable)?.resetDas()
}

fun BoardController.addGarbageIfSupported(lines: Int, garbageBlockId: Int) {
    (this as? GarbageCapable)?.addGarbage(lines, garbageBlockId)
}

fun BoardController.collapseIfSupported() {
    (this as? CollapseCapable)?.collapseFullLines()
}