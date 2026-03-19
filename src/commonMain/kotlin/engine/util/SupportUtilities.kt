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

fun PieceController?.handleDASIfSupported(delta: Double, currentDirection: Int?) {
    (this as? DasCapable)?.handleDAS(delta, currentDirection)
}

fun PieceController?.applyGravityIfSupported(delta: Double, gravitySpeed: Double) {
    (this as? GravityCapable)?.handleGravity(delta, gravitySpeed)
}

fun PieceController?.clipIfSupported() {
    (this as? ClipCapable)?.clip()
}

fun PieceController?.advanceLockIfSupported(delta: Double, onLock: () -> Unit): Boolean {
    val lockCap = this as? LockDelayCapable
    if (lockCap != null) {
        return lockCap.handleLockDelay(delta, onLock)
    }
    return false
}

fun PieceController?.getLockResetsRemainingIfSupported(): Int =
    (this as? LockDelayCapable)?.lockResetsRemaining ?: 0

fun PieceController?.updateGhostIfSupported() {
    (this as? GhostCapable)?.updateGhost()
}

fun PieceController?.getGhostRowIfSupported(): Int? {
    return (this as? GhostCapable)?.ghostRow
}

fun PieceController?.holdIfSupported() {
    (this as? HoldCapable)?.hold()
}

fun PieceController?.getHeldPieceIfSupported(): Piece? {
    return (this as? HoldCapable)?.heldPiece
}

fun PieceController?.hardDropIfSupported() {
    (this as? HardDropCapable)?.hardDrop()
}

fun PieceController?.softDropIfSupported(deltaTime: Double, gravitySpeed: Double) {
    (this as? SoftDropCapable)?.softDrop(deltaTime, gravitySpeed)
}

fun PieceController?.resetDASifSupported() {
    (this as? DasCapable)?.resetDas()
}

fun BoardController.addGarbageIfSupported(lines: Int, garbageBlockId: Int) {
    (this as? GarbageCapable)?.addGarbage(lines, garbageBlockId)
}

fun BoardController.collapseIfSupported() {
    (this as? CollapseCapable)?.collapseFullLines()
}

fun PieceController?.bufferRotationIfSupported(rotation: Rotation, isFreshPress: Boolean = true) {
    (this as? InitialActionsCapable)?.bufferRotation(rotation, isFreshPress)
}

fun PieceController?.bufferHoldIfSupported(isFreshPress: Boolean = true) {
    (this as? InitialActionsCapable)?.bufferHold(isFreshPress)
}

fun PieceController?.tickInputBufferIfSupported(delta: Double) {
    (this as? InputBufferCapable)?.tickInputBuffer(delta)
}

fun PieceController?.getLastKickIndexIfSupported(): Int =
    (this as? SpinTrackingCapable)?.lastKickIndex ?: 0

fun PieceController?.clearActionBufferIfSupported() {
    (this as? InitialActionsCapable)?.clearActionBuffer()
}