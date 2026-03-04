package controller.defaults

import controller.PieceController
import model.AppLog
import model.Board
import model.DasState
import model.events.GameEvent
import model.GameSettings
import model.GameTimers
import model.MovingPiece
import model.Piece
import model.Rotation
import model.defaults.DefaultMovingPiece
import model.events.EventHandler
import model.events.GameEvent.GameOver
import model.events.GameEvent.NewPiece
import model.events.GameEvent.PieceHeld
import model.events.GameEvent.PieceRotated
import model.events.GameEvent.SoftDrop
import util.CollisionUtils.checkCollisionWithBoard

class DefaultGuidelinePieceController<T : Piece>(
    private val board: Board,
    private val settings: GameSettings,
    private val gameTimers: GameTimers
) : PieceController<T> {
    companion object {
        private const val SOFT_DROP_PRECISION_EPSILON = 0.001f
    }

    override var heldPiece: T? = null
    override var currentPiece: MovingPiece<T>? = null
    override var ghostRow: Int = 0
    override var wasRotated = false

    private var dasState: DasState = DasState.IDLE
    private var lockResets: Int = 0
    private var canHold = true

    override fun handleDAS(delta: Float, currentDirection: Int?) {
        val dir = currentDirection ?: return
        gameTimers.dasTimer += delta

        when (dasState) {
            DasState.IDLE -> return

            DasState.DELAY -> {
                if (gameTimers.dasTimer >= settings.dasDelay) {
                    dasState = DasState.REPEAT
                    gameTimers.dasTimer -= settings.dasDelay
                }
            }

            DasState.REPEAT -> {
                while (gameTimers.dasTimer >= settings.arrDelay) {
                    if (!move(0, dir)) {
                        gameTimers.dasTimer = 0f
                        break
                    }
                    gameTimers.dasTimer -= settings.arrDelay
                }
            }
        }
        updateGhost()
    }

    override fun resetDas() {
        dasState = DasState.DELAY
        gameTimers.dasTimer = 0f
    }

    override fun handleGravity(currentLevel: Int, delta: Float) {
        val gravitySpeed = settings.gravityBase - (currentLevel - 1) * settings.gravityIncrement

        val effectiveGravity = gravitySpeed.coerceAtLeast(10f)

        gameTimers.dropTimer += delta

        if (gameTimers.dropTimer >= effectiveGravity) {
            if (move(1, 0)) {
                wasRotated = false
                gameTimers.lockTimer = 0f
                gameTimers.dropTimer -= effectiveGravity
            } else {
                gameTimers.dropTimer = 0f
            }
        }
        updateGhost()
    }


    override fun spawn(piece: T): MovingPiece<T>? {
        AppLog.debug { "Spawning piece: ${piece.name}" }
        val newPiece = DefaultMovingPiece(
            piece = piece,
            pieceCol = (board.cols / 2) - (piece.shape.cols / 2),
        )

        if (checkCollisionWithBoard(board, newPiece.shape, newPiece.pieceRow, newPiece.pieceCol)) {
            EventHandler.publish(GameOver.topic, GameOver(false, settings.goalType))
            return null
        }

        currentPiece = newPiece
        canHold = true
        wasRotated = false
        lockResets = 0
        gameTimers.lockTimer = 0f

        updateGhost()
        EventHandler.publish(NewPiece.topic, NewPiece(newPiece.piece))
        return newPiece
    }

    override fun hardDrop() {
        currentPiece?.let { piece ->
            val distance = ghostRow - piece.pieceRow
            piece.pieceRow = ghostRow
            gameTimers.lockTimer = settings.lockDelay
            EventHandler.publish(GameEvent.HardDrop.topic, GameEvent.HardDrop(distance))
        }
    }

    override fun softDrop(deltaTime: Float) {
        AppLog.debug { "SOFT_DROP: Configured Delay: ${settings.softDropDelay}" }
        AppLog.debug { "SOFT_DROP: State - Timer: ${gameTimers.softDropTimer}, Delta: $deltaTime, Delay: ${settings.softDropDelay}" }
        gameTimers.softDropTimer += deltaTime
        var dropLines = 0
        if (settings.softDropDelay <= SOFT_DROP_PRECISION_EPSILON) {
            while (move(1, 0)) {
                dropLines++
                gameTimers.dropTimer = 0.0f
                wasRotated = false
            }
        } else {
            while (gameTimers.softDropTimer >= settings.softDropDelay) {
                AppLog.debug { "SOFT_DROP: Dropping Piece with delay: ${settings.softDropDelay}" }
                if (move(1, 0)) {
                    dropLines++
                    gameTimers.dropTimer = 0.0f
                    wasRotated = false
                    gameTimers.softDropTimer -= settings.softDropDelay
                } else {
                    AppLog.debug { "SOFT_DROP: Movement blocked (Hit floor/stack)" }
                    gameTimers.softDropTimer = 0.0f
                    break
                }
            }
        }

        if (dropLines > 0) {
            EventHandler.publish(SoftDrop.topic, SoftDrop(dropLines))
        }
    }

    override fun move(targetRow: Int, targetCol: Int): Boolean {
        val moving = currentPiece ?: return false
        if (canMove(moving, targetRow, targetCol)) {
            moving.move(moving.pieceRow + targetRow, moving.pieceCol + targetCol)
            return true
        }
        return false
    }

    override fun rotate(rotation: Rotation): Boolean {
        val piece = currentPiece ?: return false
        if (rotation == Rotation.ROTATE_180 && !settings.is180Enabled) return false

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
                wasRotated = true
                EventHandler.publish(PieceRotated.topic, PieceRotated(piece.piece, piece.rotationState))
                return true
            }
        }
        return false
    }

    private fun getTopLeftFromCenter(centerRow: Int, centerCol: Int, piece: Piece): Pair<Int, Int> {
        val (rowOffset, colOffset) = piece.getRotationCenter()
        return Pair(centerRow - rowOffset, centerCol - colOffset)
    }


    override fun holdPiece(getNextPiece: () -> T) {
        if (!settings.isHoldEnabled || !canHold || currentPiece == null) return

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
        EventHandler.publish(PieceHeld.topic, PieceHeld(heldPiece!!))
        canHold = false
    }

    override fun clearPiece() {
        currentPiece = null
    }

    private fun resetLockTimer() {
        if (lockResets < settings.maxLockResets) {
            gameTimers.lockTimer = 0.0f
            lockResets++
            AppLog.debug { "Lock timer resets $lockResets" }
        }
    }

    override fun handleLockDelay(deltaTime: Float, onLock: () -> Unit) {
        if (currentPiece != null && !canMove(currentPiece!!, 1, 0)) {
            gameTimers.lockTimer += deltaTime
            if (gameTimers.lockTimer >= settings.lockDelay) {
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
}