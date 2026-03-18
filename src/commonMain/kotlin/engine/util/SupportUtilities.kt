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
import engine.controller.InitialActionsCapable
import engine.controller.InputBufferCapable
import engine.controller.LockDelayCapable
import engine.controller.PieceController
import engine.controller.SoftDropCapable
import engine.controller.SpinTrackingCapable
import engine.model.Piece
import engine.model.Rotation

fun <T : Piece> PieceController<T>?.handleDASIfSupported(delta: Double, currentDirection: Int?) {
    (this as? DasCapable)?.handleDAS(delta, currentDirection)
}

fun <T : Piece> PieceController<T>?.applyGravityIfSupported(delta: Double, gravitySpeed: Double) {
    (this as? GravityCapable)?.handleGravity(delta, gravitySpeed)
}

fun <T : Piece> PieceController<T>?.clipIfSupported() {
    (this as? ClipCapable)?.clip()
}

fun <T : Piece> PieceController<T>?.advanceLockIfSupported(delta: Double, onLock: () -> Unit): Boolean {
    val lockCap = this as? LockDelayCapable
    if (lockCap != null) {
        return lockCap.handleLockDelay(delta, onLock)
    }
    return false
}

fun <T : Piece> PieceController<T>?.getLockResetsRemainingIfSupported(): Int =
    (this as? LockDelayCapable)?.lockResetsRemaining ?: 0

fun <T : Piece> PieceController<T>?.updateGhostIfSupported() {
    (this as? GhostCapable)?.updateGhost()
}

fun <T : Piece> PieceController<T>?.getGhostRowIfSupported(): Int? {
    return (this as? GhostCapable)?.ghostRow
}

fun <T : Piece> PieceController<T>?.holdIfSupported() {
    (this as? HoldCapable<T>)?.hold()
}

fun <T : Piece> PieceController<T>?.getHeldPieceIfSupported(): T? {
    return (this as? HoldCapable<T>)?.heldPiece
}

fun <T : Piece> PieceController<T>?.hardDropIfSupported() {
    (this as? HardDropCapable)?.hardDrop()
}

fun <T : Piece> PieceController<T>?.softDropIfSupported(deltaTime: Double, gravitySpeed: Double) {
    (this as? SoftDropCapable)?.softDrop(deltaTime, gravitySpeed)
}

fun <T : Piece> PieceController<T>?.resetDASifSupported() {
    (this as? DasCapable)?.resetDas()
}

fun BoardController.addGarbageIfSupported(lines: Int, garbageBlockId: Int) {
    (this as? GarbageCapable)?.addGarbage(lines, garbageBlockId)
}

fun BoardController.collapseIfSupported() {
    (this as? CollapseCapable)?.collapseFullLines()
}

fun <T : Piece> PieceController<T>?.bufferRotationIfSupported(rotation: Rotation, isFreshPress: Boolean = true) {
    (this as? InitialActionsCapable)?.bufferRotation(rotation, isFreshPress)
}

fun <T : Piece> PieceController<T>?.bufferHoldIfSupported(isFreshPress: Boolean = true) {
    (this as? InitialActionsCapable)?.bufferHold(isFreshPress)
}

fun <T : Piece> PieceController<T>?.tickInputBufferIfSupported(delta: Double) {
    (this as? InputBufferCapable)?.tickInputBuffer(delta)
}

fun <T : Piece> PieceController<T>?.getLastKickIndexIfSupported(): Int =
    (this as? SpinTrackingCapable)?.lastKickIndex ?: 0

fun <T : Piece> PieceController<T>?.clearActionBufferIfSupported() {
    (this as? InitialActionsCapable)?.clearActionBuffer()
}