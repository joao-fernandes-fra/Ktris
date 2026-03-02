package controller.defaults

import controller.PieceController
import model.AppLog
import model.Board
import model.DasState
import model.GameSettings
import model.GameEvent
import model.GameEventBus
import model.GameTimers
import model.MovingPiece
import model.Piece
import model.Rotation
import model.defaults.DefaultMovingPiece
import util.CollisionUtils.checkCollisionWithBoard
import kotlin.math.abs

class DefaultPieceController<T : Piece>(
    private val board: Board,
    private val settings: GameSettings,
    private val gameTimers: GameTimers,
    private val gameEventBus: GameEventBus
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
                gameTimers.lockTimer = 0.0f
                gameTimers.dropTimer -= effectiveGravity
            } else {
                gameTimers.dropTimer = 0.0f
            }
        }
        updateGhost()
    }


    override fun spawn(piece: T): MovingPiece<T>? {
        AppLog.debug { "Spawning piece: ${piece.name}" }
        val newPiece = DefaultMovingPiece(
            piece = piece,
            pieceCol = (board.cols / 2) - (piece.shape.cols / 2),
            pieceRow = 0
        )

        if (checkCollisionWithBoard(board, newPiece.shape, newPiece.pieceRow, newPiece.pieceCol)) {
            gameEventBus.post(GameEvent.GameOver(false, settings.goalType))
            return null
        }

        currentPiece = newPiece
        canHold = true
        wasRotated = false
        lockResets = 0
        gameTimers.lockTimer = 0.0f

        updateGhost()
        gameEventBus.post(GameEvent.NewPiece(newPiece.piece))
        return newPiece
    }

    override fun hardDrop() {
        currentPiece?.let { piece ->
            val distance = ghostRow - piece.pieceRow
            piece.pieceRow = ghostRow
            gameTimers.lockTimer = settings.lockDelay
            gameEventBus.post(GameEvent.HardDrop(distance))
        }
    }

    override fun softDrop(deltaTime: Float) {
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
                if (move(1, 0)) {
                    dropLines++
                    gameTimers.dropTimer = 0.0f
                    wasRotated = false
                    gameTimers.softDropTimer -= settings.softDropDelay
                } else {
                    gameTimers.softDropTimer = 0.0f
                    break
                }
            }
        }

        gameEventBus.post(GameEvent.SoftDrop(dropLines))
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
        val moving = currentPiece ?: return false
        if (rotation == Rotation.ROTATE_180 && !settings.is180Enabled) return false

        val (candidateShape, _) = moving.projectRotation(rotation)
        val tests = moving.piece.getKickTable(rotation, moving.rotationState)

        val (rCenter, cCenter) = moving.piece.getRotationCenter()
        val originRow = moving.pieceRow + rCenter
        val originCol = moving.pieceCol + cCenter

        data class Candidate(val topLeftRow: Int, val topLeftCol: Int, val offsetX: Int, val offsetY: Int)

        val validCandidates = mutableListOf<Candidate>()

        for ((offsetX, offsetY) in tests) {
            val testOriginCol = originCol + offsetX
            val testOriginRow = originRow + offsetY

            val topLeftCol = testOriginCol - cCenter
            val topLeftRow = testOriginRow - rCenter

            val isCollision = checkCollisionWithBoard(board, candidateShape, topLeftRow, topLeftCol)
            AppLog.debug { "Kick test offset=($offsetX,$offsetY) test=($topLeftCol,$topLeftRow) collision=$isCollision" }
            if (!isCollision) {
                validCandidates.add(Candidate(topLeftRow, topLeftCol, offsetX, offsetY))
            }
        }

        if (validCandidates.isEmpty()) return false

        val chosen = validCandidates.minWithOrNull(
            compareBy<Candidate> { -it.topLeftRow }
                .thenBy { abs(it.topLeftCol - moving.pieceCol) }
                .thenBy { abs(it.topLeftRow - moving.pieceRow) + abs(it.topLeftCol - moving.pieceCol) }
        ) ?: validCandidates.first()


        moving.rotateShape(candidateShape, chosen.topLeftRow, chosen.topLeftCol, rotation)
        gameTimers.lockTimer = 0.0f
        resetLockTimer()
        wasRotated = true
        gameEventBus.post(GameEvent.PieceRotated(moving.piece, moving.rotationState))
        return true
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
        gameEventBus.post(GameEvent.PieceHeld(heldPiece!!))
        canHold = false
    }

    override fun clearPiece() {
        currentPiece = null
    }

    private fun resetLockTimer() {
        if (lockResets < settings.maxLockResets) {
            gameTimers.lockTimer = 0.0f
            lockResets++
        }
    }

    override fun handleLockDelay(deltaTime: Float, onLock: () -> Unit) {
        if (currentPiece != null && !canMove(currentPiece!!, 1, 0)) {
            gameTimers.lockTimer += deltaTime
            if (gameTimers.lockTimer >= settings.lockDelay) {
                onLock()
            }
        } else {
            gameTimers.lockTimer = 0.0f
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