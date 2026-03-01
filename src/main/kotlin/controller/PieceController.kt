package controller

import model.defaults.DefaultMovingPiece
import model.Piece
import model.Rotation

interface PieceController<T : Piece> {
    fun getHeldPiece(): T?
    fun getCurrentPiece(): DefaultMovingPiece<T>?
    fun getGhostRow(): Int
    fun wasRotated(): Boolean
    fun handleDAS(delta: Float, currentDirection: Int?)
    fun resetDas()
    fun handleGravity(currentLevel: Int, delta: Float)
    fun spawn(piece: T)
    fun hardDrop()
    fun softDrop(deltaTime: Float)
    fun move(targetRow: Int, targetCol: Int): Boolean
    fun rotate(rotation: Rotation): Boolean
    fun holdPiece(getNextPiece: () -> T)
    fun clearPiece()
    fun handleLockDelay(deltaTime: Float, onLock: () -> Unit)
}