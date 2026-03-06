package engine.controller.defaults

import engine.controller.PieceController
import engine.model.AppLog
import engine.model.Board
import engine.model.DasState
import engine.model.GameTimers
import engine.model.GlobalGameSettings
import engine.model.LastPieceAction
import engine.model.MovingPiece
import engine.model.Piece
import engine.model.PlayerSettings
import engine.model.Rotation
import engine.model.defaults.DefaultMovingPiece
import engine.model.events.EventOrchestrator
import engine.model.events.GameEvent
import engine.model.events.GameEvent.GameOver
import engine.model.events.GameEvent.NewPiece
import engine.model.events.GameEvent.PieceHeld
import engine.model.events.GameEvent.PieceRotated
import engine.model.events.GameEvent.SoftDrop
import engine.model.events.currentGameId
import engine.util.CollisionUtils.checkCollisionWithBoard

class DefaultPieceProvider<T : Piece>(
    private val board: Board,
    private val playerSettings: PlayerSettings,
    private val globalGameSettings: GlobalGameSettings,
    private val gameTimers: GameTimers,
    private val gameId: String
) : PieceController<T> {
    companion object {
        private const val SOFT_DROP_PRECISION_EPSILON = 0.001f
    }

    override var heldPiece: T? = null
    override var currentPiece: MovingPiece<T>? = null
    override var ghostRow: Int = 0
    override var lastAction: LastPieceAction = LastPieceAction.NONE

    private var dasState: DasState = DasState.IDLE
    private var lockResets: Int = 0
    private var canHold = true

    override suspend fun handleDAS(delta: Float, currentDirection: Int?) {
        val dir = currentDirection ?: return
        gameTimers.dasTimer += delta

        when (dasState) {
            DasState.IDLE -> return

            DasState.DELAY -> {
                if (gameTimers.dasTimer >= playerSettings.dasDelay) {
                    dasState = DasState.REPEAT
                    gameTimers.dasTimer -= playerSettings.dasDelay
                }
            }

            DasState.REPEAT -> {
                while (gameTimers.dasTimer >= playerSettings.arrDelay) {
                    if (!movePiece(0, dir)) {
                        gameTimers.dasTimer = 0f
                        break
                    }
                    gameTimers.dasTimer -= playerSettings.arrDelay
                }
            }
        }
        updateGhost()
    }

    override suspend fun resetDas() {
        dasState = DasState.DELAY
        gameTimers.dasTimer = 0f
    }

    override suspend fun handleGravity(currentLevel: Int, delta: Float) {
        val gravitySpeed = globalGameSettings.gravityBase - (currentLevel - 1) * globalGameSettings.gravityIncrement

        val effectiveGravity = gravitySpeed.coerceAtLeast(10f)

        gameTimers.dropTimer += delta

        if (gameTimers.dropTimer >= effectiveGravity) {
            if (movePiece(1, 0)) {
                gameTimers.lockTimer = 0f
                gameTimers.dropTimer -= effectiveGravity
            } else {
                gameTimers.dropTimer = 0f
            }
        }
        updateGhost()
    }


    override suspend fun spawn(piece: T): MovingPiece<T>? {
        AppLog.debug { "Spawning piece: ${piece.name}" }
        val newPiece = DefaultMovingPiece(
            piece = piece,
            pieceCol = (board.cols / 2) - (piece.shape.cols / 2),
        )

        if (checkCollisionWithBoard(board, newPiece.shape, newPiece.pieceRow, newPiece.pieceCol)) {
            EventOrchestrator.publish(GameOver(false, globalGameSettings.goalType, gameId))
            return null
        }

        currentPiece = newPiece
        canHold = true
        lastAction = LastPieceAction.NONE
        lockResets = 0
        gameTimers.lockTimer = 0f

        updateGhost()
        EventOrchestrator.publish(NewPiece(newPiece.piece, gameId))
        return newPiece
    }

    override suspend fun hardDrop() {
        currentPiece?.let { piece ->
            val distance = ghostRow - piece.pieceRow
            piece.pieceRow = ghostRow
            gameTimers.lockTimer = playerSettings.lockDelay
            EventOrchestrator.publish(GameEvent.HardDrop(distance, gameId))
        }
    }

    override suspend fun softDrop(deltaTime: Float) {
        AppLog.debug { "SOFT_DROP: Configured Delay: ${playerSettings.softDropDelay}" }
        AppLog.debug { "SOFT_DROP: State - Timer: ${gameTimers.softDropTimer}, Delta: $deltaTime, Delay: ${playerSettings.softDropDelay}" }
        gameTimers.softDropTimer += deltaTime
        var dropLines = 0
        if (playerSettings.softDropDelay <= SOFT_DROP_PRECISION_EPSILON) {
            while (movePiece(1, 0)) {
                dropLines++
                gameTimers.dropTimer = 0.0f

            }
        } else {
            while (gameTimers.softDropTimer >= playerSettings.softDropDelay) {
                AppLog.debug { "SOFT_DROP: Dropping Piece with delay: ${playerSettings.softDropDelay}" }
                if (movePiece(1, 0)) {
                    dropLines++
                    gameTimers.dropTimer = 0.0f

                    gameTimers.softDropTimer -= playerSettings.softDropDelay
                } else {
                    AppLog.debug { "SOFT_DROP: Movement blocked (Hit floor/stack)" }
                    gameTimers.softDropTimer = 0.0f
                    break
                }
            }
        }

        if (dropLines > 0) {
            EventOrchestrator.publish(SoftDrop(dropLines, gameId))
        }
    }

    override suspend fun move(targetRow: Int, targetCol: Int): Boolean {
        if (movePiece(targetRow, targetCol)) {
            lastAction = LastPieceAction.MOVE
            return true
        }
        return false
    }

    private fun movePiece(targetRow: Int, targetCol: Int): Boolean {
        val moving = currentPiece ?: return false
        if (canMove(moving, targetRow, targetCol)) {
            moving.move(moving.pieceRow + targetRow, moving.pieceCol + targetCol)
            return true
        }
        return false
    }

    override suspend fun rotate(rotation: Rotation): Boolean {
        val piece = currentPiece ?: return false
        if (rotation == Rotation.ROTATE_180 && !playerSettings.is180Enabled) return false

        val (candidateShape, newRotationState) = piece.projectRotation(rotation)
        val kickOffsets = piece.piece.getKickTable(rotation, newRotationState)
        val (centerRowOffset, centerColOffset) = piece.piece.getRotationCenter()
        val currentCenterRow = piece.pieceRow + centerRowOffset
        val currentCenterCol = piece.pieceCol + centerColOffset
        for ((deltaCol, deltaRow) in kickOffsets) {
            val newCenterRow = currentCenterRow + deltaRow
            val newCenterCol = currentCenterCol + deltaCol
            val (topLeftRow, topLeftCol) = getTopLeftFromCenter(newCenterRow, newCenterCol, piece.piece)
            val hasCollision = checkCollisionWithBoard(board, candidateShape, topLeftRow, topLeftCol)

            if (!hasCollision) {
                piece.rotateShape(candidateShape, topLeftRow, topLeftCol, rotation)
                gameTimers.lockTimer = 0.0f
                resetLockTimer()
                lastAction = LastPieceAction.ROTATE
                EventOrchestrator.publish(PieceRotated(piece.piece, piece.rotationState, gameId))
                return true
            }
        }
        return false
    }

    private fun getTopLeftFromCenter(centerRow: Int, centerCol: Int, piece: Piece): Pair<Int, Int> {
        val (rowOffset, colOffset) = piece.getRotationCenter()
        return Pair(centerRow - rowOffset, centerCol - colOffset)
    }


    override suspend fun holdPiece(getNextPiece: () -> T) {
        if (!playerSettings.isHoldEnabled || !canHold || currentPiece == null) return

        val pieceToHold = currentPiece!!.piece
        if (heldPiece == null) {
            heldPiece = pieceToHold
            spawn(getNextPiece())
        } else {
            val next = heldPiece!!
            heldPiece = pieceToHold
            spawn(next)
        }
        AppLog.info { "Piece held: ${heldPiece?.name}" }
        EventOrchestrator.publish(PieceHeld(heldPiece!!, gameId))
        canHold = false
    }

    override suspend fun clearPiece() {
        currentPiece = null
    }

    private fun resetLockTimer() {
        if (lockResets < playerSettings.maxLockResets) {
            gameTimers.lockTimer = 0.0f
            lockResets++
            AppLog.debug { "Lock timer resets $lockResets" }
        }
    }

    override suspend fun handleLockDelay(deltaTime: Float, onLock: suspend () -> Unit) {
        if (currentPiece != null && !canMove(currentPiece!!, 1, 0)) {
            gameTimers.lockTimer += deltaTime
            if (gameTimers.lockTimer >= playerSettings.lockDelay) {
                onLock()
            }
        } else {
            gameTimers.lockTimer = 0f
        }
    }

    private fun canMove(piece: MovingPiece<T>, dRow: Int, dCol: Int, row: Int = piece.pieceRow): Boolean {
        return !checkCollisionWithBoard(board, piece.shape, row + dRow, piece.pieceCol + dCol)
    }

    private fun updateGhost() {
        currentPiece?.let { piece ->
            var testRow = piece.pieceRow
            while (canMove(piece, 1, 0, testRow)) {
                testRow++
            }
            ghostRow = testRow
        }
    }

    override fun reset() {
        heldPiece = null
        currentPiece = null
        ghostRow = 0

        lockResets = 0
        dasState = DasState.IDLE
        canHold = true
    }
}