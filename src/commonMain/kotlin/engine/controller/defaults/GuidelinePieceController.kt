package engine.controller.defaults

import engine.controller.BagRandomizer
import engine.controller.ClipCapable
import engine.controller.DasCapable
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
import engine.model.Board
import engine.model.DasPreservation
import engine.model.DasState
import engine.model.GameOverReason
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
import engine.model.events.GameEvent.GameOver
import engine.model.events.GameEvent.NewPiece
import engine.model.events.GameEvent.PieceHeld
import engine.model.events.GameEvent.PieceRotated
import engine.model.events.GameEvent.SoftDrop
import engine.model.events.MoveSource
import engine.util.CollisionUtils.checkCollisionWithBoard

class GuidelinePieceController<T : Piece>(
    private val board: Board,
    private val bagRandomizer: BagRandomizer<T>,
    private val playerSettings: PlayerSettings,
    private val globalGameSettings: GameSettings,
    private val gameTimers: GameTimers,
    private val gameId: String
) : PieceController<T>, DasCapable,
    GravityCapable,
    SoftDropCapable,
    HardDropCapable,
    HoldCapable<T>,
    InitialActionsCapable,
    InputBufferCapable,
    ClipCapable,
    LockDelayCapable,
    GhostCapable,
    SpinTrackingCapable {
    companion object {
        private const val SOFT_DROP_PRECISION_EPSILON = 0.001f
        private const val ROTATION_BUFFER_WINDOW = 133.0
    }

    init {
        EventOrchestrator.subscribeForGameId<GameEvent.LevelUp>(gameId) {
            currentLevel = it.newLevel
        }
    }

    private var dasState: DasState = DasState.IDLE
    private var lockResets: Int = 0
    private var lowestRow: Int = Int.MAX_VALUE
    private var canHold = true
    private var bufferedRotation: Rotation? = null
    private var rotationBufferTimer: Double = 0.0
    private var currentLevel = 0
    override var heldPiece: T? = null
    override var currentPiece: MovingPiece<T>? = null
    override var ghostRow: Int = 0
    override var lastAction: LastPieceAction = LastPieceAction.NONE
    override val rotationBufferWindow: Double = ROTATION_BUFFER_WINDOW
    override var holdBuffered: Boolean = false
    override var lastKickIndex = 0
    override var lastKickWasFinal = false
    override val lockResetsRemaining: Int get() = playerSettings.maxLockResets - lockResets

    override fun getNextPieces(previewSize: Int): List<T> {
        return bagRandomizer.getPreview(previewSize)
    }

    override fun handleDAS(delta: Double, currentDirection: Int?) {
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
                if (playerSettings.arrDelay <= 0.0) {
                    while (movePiece(0, dir, MoveSource.PLAYER)) { /* move until blocked */ }
                    gameTimers.dasTimer = 0.0
                } else {
                    while (gameTimers.dasTimer >= playerSettings.arrDelay) {
                        if (!movePiece(0, dir, MoveSource.PLAYER)) {
                            gameTimers.dasTimer = 0.0
                            break
                        }
                        gameTimers.dasTimer -= playerSettings.arrDelay
                    }
                }
            }
        }
        updateGhost()
    }

    override fun resetDas() {
        dasState = DasState.DELAY
        gameTimers.dasTimer = 0.0
    }

    override fun preserveDas() {
        when (playerSettings.dasPreservation) {
            DasPreservation.FULL -> {}
            DasPreservation.CHARGE_ONLY -> {
                if (dasState == DasState.REPEAT) dasState = DasState.DELAY
            }

            DasPreservation.RESET -> resetDas()
        }
    }

    override fun handleGravity(delta: Double) {
        val gravitySpeed = globalGameSettings.gravityBase - (currentLevel - 1) * globalGameSettings.gravityIncrement

        gameTimers.dropTimer += delta
        if (gameTimers.dropTimer >= gravitySpeed) {
            if (movePiece(1, 0, MoveSource.GRAVITY)) {
                gameTimers.lockTimer = 0.0
                gameTimers.dropTimer -= gravitySpeed
            } else {
                gameTimers.dropTimer = 0.0
            }
        }
        updateGhost()
    }


    override fun spawn(piece: T?): MovingPiece<T>? {
        val nextPiece = piece ?: bagRandomizer.getNextPiece()
        Logger.debug { "Spawning piece: ${nextPiece.name}" }
        val newPiece = DefaultMovingPiece(
            piece = nextPiece,
            pieceCol = (board.cols / 2) - (nextPiece.shape.cols / 2),
        )

        if (checkCollisionWithBoard(board, newPiece.shape, newPiece.pieceRow, newPiece.pieceCol)) {
            EventOrchestrator.publish(GameOver(GameOverReason.BLOCK_OUT, globalGameSettings.goalType, gameId))
            return null
        }

        currentPiece = newPiece
        canHold = true
        lastAction = LastPieceAction.NONE
        lowestRow = Int.MIN_VALUE
        lockResets = 0
        gameTimers.lockTimer = 0.0

        preserveDas()

        if (holdBuffered && playerSettings.isHoldEnabled) {
            holdBuffered = false
            bufferedRotation = null
            rotationBufferTimer = 0.0
            updateGhost()
            EventOrchestrator.publish(NewPiece(newPiece.piece, gameId))
            hold()
            return currentPiece
        }

        val irs = bufferedRotation
        if (irs != null) {
            bufferedRotation = null
            rotationBufferTimer = 0.0
            rotate(irs)
        }

        updateGhost()
        EventOrchestrator.publish(NewPiece(newPiece.piece, gameId))
        return newPiece
    }

    override fun clip() {
        val piece = currentPiece ?: return
        var targetRow = piece.pieceRow

        while (targetRow > 0 && !canPlace(piece, targetRow, piece.pieceCol)) {
            targetRow--
        }

        if (targetRow != piece.pieceRow) {
            piece.pieceRow = targetRow
            updateGhost()
        }
    }

    override fun hardDrop() {
        currentPiece?.let { piece ->
            val distance = ghostRow - piece.pieceRow
            piece.pieceRow = ghostRow
            gameTimers.lockTimer = playerSettings.lockDelay
            EventOrchestrator.publish(GameEvent.HardDrop(distance, gameId))
        }
    }

    override fun softDrop(deltaTime: Double) {
        Logger.debug { "SOFT_DROP: Configured Delay: ${playerSettings.softDropDelay}" }
        Logger.debug { "SOFT_DROP: State - Timer: ${gameTimers.softDropTimer}, Delta: $deltaTime, Delay: ${playerSettings.softDropDelay}" }
        gameTimers.softDropTimer += deltaTime
        var dropLines = 0
        if (playerSettings.softDropDelay <= SOFT_DROP_PRECISION_EPSILON) {
            while (movePiece(1, 0, MoveSource.SOFT_DROP)) {
                dropLines++
                gameTimers.dropTimer = 0.0

            }
        } else {
            while (gameTimers.softDropTimer >= playerSettings.softDropDelay) {
                Logger.debug { "SOFT_DROP: Dropping Piece with delay: ${playerSettings.softDropDelay}" }
                if (movePiece(1, 0, MoveSource.SOFT_DROP)) {
                    dropLines++
                    gameTimers.dropTimer = 0.0

                    gameTimers.softDropTimer -= playerSettings.softDropDelay
                } else {
                    Logger.debug { "SOFT_DROP: Movement blocked (Hit floor/stack)" }
                    gameTimers.softDropTimer = 0.0
                    break
                }
            }
        }

        if (dropLines > 0) {
            EventOrchestrator.publish(SoftDrop(dropLines, gameId))
        }
    }

    override fun move(targetRow: Int, targetCol: Int): Boolean {
        if (movePiece(targetRow, targetCol, MoveSource.PLAYER)) {
            lastAction = LastPieceAction.MOVE
            return true
        }
        return false
    }

    private fun movePiece(targetRow: Int, targetCol: Int, source: MoveSource): Boolean {
        val moving = currentPiece ?: return false
        if (canMove(moving, targetRow, targetCol)) {
            moving.move(moving.pieceRow + targetRow, moving.pieceCol + targetCol)
            checkLowestRow(moving)
            EventOrchestrator.publish(
                GameEvent.PieceMoved(targetRow, targetCol, moving.pieceRow, moving.pieceCol, source, gameId)
            )
            return true
        }
        return false
    }

    override fun rotate(rotation: Rotation): Boolean {
        val piece = currentPiece ?: return false
        if (rotation == Rotation.ROTATE_180 && !playerSettings.is180Enabled) return false

        val (candidateShape, _) = piece.projectRotation(rotation)
        val kickOffsets = piece.piece.getKickTable(rotation, piece.rotationState)
        val (centerRowOffset, centerColOffset) = piece.piece.getRotationCenter()
        val currentCenterRow = piece.pieceRow + centerRowOffset
        val currentCenterCol = piece.pieceCol + centerColOffset

        for ((index, kick) in kickOffsets.withIndex()) {
            val (deltaCol, deltaRow) = kick
            val newCenterRow = currentCenterRow + deltaRow
            val newCenterCol = currentCenterCol + deltaCol
            val (topLeftRow, topLeftCol) = getTopLeftFromCenter(newCenterRow, newCenterCol, piece.piece)

            if (!checkCollisionWithBoard(board, candidateShape, topLeftRow, topLeftCol)) {
                lastKickIndex = index
                lastKickWasFinal = index == kickOffsets.size - 1
                piece.rotateShape(candidateShape, topLeftRow, topLeftCol, rotation)
                gameTimers.lockTimer = 0.0
                resetLockTimer()
                lastAction = LastPieceAction.ROTATE
                bufferedRotation = null
                rotationBufferTimer = 0.0
                EventOrchestrator.publish(PieceRotated(piece.piece, piece.rotationState, gameId))
                return true
            }
        }
        lastKickWasFinal = false
        lastKickIndex = 0
        bufferedRotation = rotation
        rotationBufferTimer = 0.0
        return false
    }

    private fun getTopLeftFromCenter(centerRow: Int, centerCol: Int, piece: Piece): Pair<Int, Int> {
        val (rowOffset, colOffset) = piece.getRotationCenter()
        return Pair(centerRow - rowOffset, centerCol - colOffset)
    }


    override fun hold() {
        if (!playerSettings.isHoldEnabled || !canHold || currentPiece == null) {
            clearActionBuffer()
            return
        }

        val pieceToHold = currentPiece!!.piece
        if (heldPiece == null) {
            heldPiece = pieceToHold
            spawn()
        } else {
            val next = heldPiece!!
            heldPiece = pieceToHold
            spawn(next)
        }
        Logger.info { "Piece held: ${heldPiece?.name}" }
        EventOrchestrator.publish(PieceHeld(heldPiece!!, gameId))
        canHold = false
    }

    override fun clearPiece() {
        currentPiece = null
        clearActionBuffer()
    }

    private fun resetLockTimer() {
        if (lockResets < playerSettings.maxLockResets) {
            gameTimers.lockTimer = 0.0
            lockResets++
        }
    }

    override fun handleLockDelay(deltaTime: Double, onLock: () -> Unit): Boolean {
        val piece = currentPiece ?: return false

        val isTouchingFloor = !canMove(piece, 1, 0)
        val isInsideBlock = checkCollisionWithBoard(board, piece.shape, piece.pieceRow, piece.pieceCol)

        if (isTouchingFloor || isInsideBlock) {
            gameTimers.lockTimer += deltaTime
            if (gameTimers.lockTimer >= playerSettings.lockDelay || isInsideBlock) {
                onLock()
                return true
            }
        } else {
            gameTimers.lockTimer = 0.0
        }
        return false
    }

    private fun canMove(piece: MovingPiece<T>, dRow: Int, dCol: Int, row: Int = piece.pieceRow): Boolean {
        return !checkCollisionWithBoard(board, piece.shape, row + dRow, piece.pieceCol + dCol)
    }

    private fun canPlace(piece: MovingPiece<T>, row: Int, col: Int): Boolean {
        return !checkCollisionWithBoard(board, piece.shape, row, col)
    }

    override fun updateGhost() {
        currentPiece?.let { piece ->
            var testRow = piece.pieceRow
            while (canMove(piece, 1, 0, testRow)) {
                testRow++
            }
            ghostRow = testRow
        }
    }

    override fun reset() {
        currentPiece = null
        bagRandomizer.reset()
        heldPiece = null
        ghostRow = 0
        lowestRow = Int.MIN_VALUE
        lockResets = 0
        dasState = DasState.IDLE
        canHold = true
        lastKickIndex = 0
        lastKickWasFinal = false
        clearActionBuffer()
    }

    override fun bufferRotation(rotation: Rotation) {
        bufferedRotation = rotation
    }

    override fun bufferHold() {
        holdBuffered = true
    }

    override fun clearActionBuffer() {
        bufferedRotation = null
        holdBuffered = false
        rotationBufferTimer = 0.0
    }

    override fun tickInputBuffer(delta: Double) {
        if (bufferedRotation != null) {
            rotationBufferTimer += delta
            if (rotationBufferTimer >= ROTATION_BUFFER_WINDOW) {
                bufferedRotation = null
                rotationBufferTimer = 0.0
            }
        }
    }

    private fun checkLowestRow(piece: MovingPiece<T>) {
        val currentBottom = piece.pieceRow + piece.shape.rows - 1
        if (currentBottom > lowestRow) {
            lowestRow = currentBottom
            lockResets = 0
            gameTimers.lockTimer = 0.0
            Logger.debug { "New lowest row $lowestRow — lock resets refreshed" }
        }
    }
}