package engine.controller.defaults

import engine.controller.BagRandomizer
import engine.controller.GravityCapable
import engine.controller.LockDelayCapable
import engine.controller.PieceController
import engine.controller.SoftDropCapable
import engine.model.Board
import engine.model.GameSettings
import engine.model.GameTimers
import engine.model.LastPieceAction
import engine.model.MovingPiece
import engine.model.Piece
import engine.model.PlayerSettings
import engine.model.Rotation
import engine.model.defaults.DefaultMovingPiece
import engine.model.defaults.Logger
import engine.model.events.EventOrchestrator
import engine.model.events.GameEvent
import engine.model.events.GameEvent.PieceRotated
import engine.util.CollisionUtils.checkCollisionWithBoard

//TODO validate this class later on / should allow extension for extra implementation
open class ClassicPieceController<T : Piece>(
    protected val board: Board,
    protected val bagRandomizer: BagRandomizer<T>,
    protected val playerSettings: PlayerSettings,
    protected val gameSettings: GameSettings,
    protected val gameTimers: GameTimers,
    protected val gameId: String
) : PieceController<T>, GravityCapable, LockDelayCapable, SoftDropCapable {

    override var currentPiece: MovingPiece<T>? = null
    override var lastAction: LastPieceAction = LastPieceAction.NONE

    private var lockResets: Int = 0

    override suspend fun getNextPieces(previewSize: Int): List<T> {
        return bagRandomizer.getPreview(previewSize)
    }

    override suspend fun spawn(piece: T?): MovingPiece<T>? {
        val next = piece ?: bagRandomizer.getNextPiece()
        Logger.debug { "Spawning piece: ${next.name}" }
        val mp = DefaultMovingPiece(piece = next, pieceCol = (board.cols / 2) - (next.shape.cols / 2))

        if (checkCollisionWithBoard(board, mp.shape, mp.pieceRow, mp.pieceCol)) {
            EventOrchestrator.publish(GameEvent.GameOver(false, gameSettings.goalType, gameId))
            return null
        }

        currentPiece = mp
        lockResets = 0
        gameTimers.lockTimer = 0.0
        EventOrchestrator.publish(GameEvent.NewPiece(mp.piece, gameId))
        return mp
    }

    override suspend fun handleGravity(delta: Double) {
        val gravitySpeed = gameSettings.gravityBase - (1 - 1) * gameSettings.gravityIncrement
        gameTimers.dropTimer += delta
        if (gameTimers.dropTimer >= gravitySpeed) {
            if (movePiece(1, 0)) {
                gameTimers.lockTimer = 0.0
                gameTimers.dropTimer -= gravitySpeed
            } else {
                gameTimers.dropTimer = 0.0
            }
        }
    }

    override suspend fun handleLockDelay(deltaTime: Double, onLock: suspend () -> Unit): Boolean {
        val piece = currentPiece ?: return false
        val isTouchingFloor = !canMove(piece, 1, 0)
        val isInsideBlock = checkCollisionWithBoard(board, piece.shape, piece.pieceRow, piece.pieceCol)

        if (isTouchingFloor || isInsideBlock) {
            onLock()
            return true
        }
        return false
    }

    override suspend fun move(targetRow: Int, targetCol: Int): Boolean {
        if (movePiece(targetRow, targetCol)) {
            lastAction = LastPieceAction.MOVE
            return true
        }
        return false
    }

    override suspend fun rotate(rotation: Rotation): Boolean {
        val piece = currentPiece ?: return false
        if (rotation == Rotation.ROTATE_180 && !playerSettings.is180Enabled) return false
        val (candidateShape, _) = piece.projectRotation(rotation)
        val topLeftRow = piece.pieceRow
        val topLeftCol = piece.pieceCol
        val hasCollision = checkCollisionWithBoard(board, candidateShape, topLeftRow, topLeftCol)
        if (hasCollision) return false
        piece.rotateShape(candidateShape, topLeftRow, topLeftCol, rotation)
        gameTimers.lockTimer = 0.0
        lastAction = LastPieceAction.ROTATE
        EventOrchestrator.publish(PieceRotated(piece.piece, piece.rotationState, gameId))
        return true
    }

    fun canMove(piece: MovingPiece<T>, dRow: Int, dCol: Int, row: Int = piece.pieceRow): Boolean {
        return !checkCollisionWithBoard(board, piece.shape, row + dRow, piece.pieceCol + dCol)
    }
    fun movePiece(dRow: Int, dCol: Int): Boolean {
        val p = currentPiece ?: return false
        if (!checkCollisionWithBoard(board, p.shape, p.pieceRow + dRow, p.pieceCol + dCol)) {
            p.move(p.pieceRow + dRow, p.pieceCol + dCol)
            lastAction = LastPieceAction.MOVE
            return true
        }
        return false
    }


    override suspend fun clearPiece() {
        currentPiece = null
    }

    override fun reset() {
        currentPiece = null
        lastAction = LastPieceAction.NONE
        lockResets = 0
        bagRandomizer.reset()
    }

    override suspend fun softDrop(deltaTime: Double) {
        if (movePiece(1, 0)) {
            gameTimers.dropTimer = 0.0
            EventOrchestrator.publish(GameEvent.SoftDrop(1, gameId))
        }
    }
}
