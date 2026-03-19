package engine.controller.defaults.piece

import engine.controller.PieceRandomizer
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
import engine.model.BufferMode
import engine.model.DasPreservation
import engine.model.GameOverReason
import engine.model.LastPieceAction
import engine.model.MatchConfig
import engine.model.MovingPiece
import engine.model.Piece
import engine.model.PieceTimers
import engine.model.PlayerConfig
import engine.model.Rotation
import engine.model.defaults.DefaultMovingPiece
import engine.util.Logger
import engine.model.events.DefaultGameEvents
import engine.model.events.DefaultGameEvents.GameOver
import engine.model.events.DefaultGameEvents.NewPiece
import engine.model.events.DefaultGameEvents.PieceHeld
import engine.model.events.DefaultGameEvents.PieceRotated
import engine.model.events.DefaultGameEvents.SoftDrop
import engine.model.events.EventOrchestrator
import engine.model.MoveSource
import engine.util.CollisionUtils.checkCollisionWithBoard

class GuidelinePieceController(
    private val board: Board,
    private val pieceRandomizer: PieceRandomizer,
    private val playerSettings: PlayerConfig,
    private val gameSettings: MatchConfig,
    private val gameId: String
) : PieceController, DasCapable, GravityCapable, SoftDropCapable, HardDropCapable, HoldCapable,
    InitialActionsCapable, InputBufferCapable, ClipCapable, LockDelayCapable, GhostCapable, SpinTrackingCapable {
    companion object {
        private const val ROTATION_BUFFER_WINDOW = 133.0
    }

    private val gameTimers: PieceTimers = PieceTimers()
    private var dasState: DasState = DasState.IDLE
    private var lockResets: Int = 0
    private var lowestRow: Int = Int.MAX_VALUE
    private var canHold = true
    private var bufferedRotation: Rotation? = null
    private var rotationBufferTimer: Double = 0.0
    override var heldPiece: Piece? = null
    override var currentPiece: MovingPiece? = null
    override var ghostRow: Int = 0
    override var lastAction: LastPieceAction = LastPieceAction.NONE
    override val rotationBufferWindow: Double = ROTATION_BUFFER_WINDOW
    override var holdBuffered: Boolean = false
    override var lastKickIndex = 0
    override val lockResetsRemaining: Int get() = gameSettings.gravity.maxLockResets - lockResets

    override fun getNextPieces(previewSize: Int): List<Piece> {
        return pieceRandomizer.getPreview(previewSize)
    }

    override fun handleDAS(delta: Double, currentDirection: Int?) {
        val dir = currentDirection ?: return
        gameTimers.dasTimer += delta

        when (dasState) {
            DasState.IDLE -> return
            DasState.DCD -> {
                gameTimers.dcdTimer += delta
                if (gameTimers.dcdTimer >= playerSettings.handling.dcdDelay) {
                    dasState = DasState.DELAY
                    gameTimers.dcdTimer = 0.0
                    gameTimers.dasTimer = 0.0
                }
            }

            DasState.DELAY -> {
                if (gameTimers.dasTimer >= playerSettings.handling.dasDelay) {
                    dasState = DasState.REPEAT
                    gameTimers.dasTimer -= playerSettings.handling.dasDelay
                }
            }

            DasState.REPEAT -> {
                if (playerSettings.handling.arrDelay <= 0.0) {
                    while (movePiece(0, dir, MoveSource.PLAYER)) { /* move until blocked */
                    }
                    gameTimers.dasTimer = 0.0
                } else {
                    while (gameTimers.dasTimer >= playerSettings.handling.arrDelay) {
                        if (!movePiece(0, dir, MoveSource.PLAYER)) {
                            gameTimers.dasTimer = 0.0
                            break
                        }
                        gameTimers.dasTimer -= playerSettings.handling.arrDelay
                    }
                }
            }
        }
        updateGhost()
    }

    override fun resetDas() {
        dasState = if (playerSettings.handling.dcdDelay > 0.0) DasState.DCD else DasState.DELAY
        gameTimers.dasTimer = 0.0
        gameTimers.dcdTimer = 0.0
    }

    override fun preserveDas() {
        when (playerSettings.handling.dasPreservation) {
            DasPreservation.FULL -> {}
            DasPreservation.CHARGE_ONLY -> {
                if (dasState == DasState.REPEAT) dasState = DasState.DELAY
            }

            DasPreservation.RESET -> resetDas()
        }
    }

    override fun handleGravity(delta: Double, gravitySpeed: Double) {
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


    override fun spawn(piece: Piece?): MovingPiece? {
        val nextPiece = piece ?: pieceRandomizer.getNextPiece()
        Logger.debug { "Spawning piece: ${nextPiece.name}" }
        val newPiece = DefaultMovingPiece(
            piece = nextPiece,
            pieceCol = (board.cols / 2) - (nextPiece.shape.cols / 2),
        )

        if (checkCollisionWithBoard(board, newPiece.shape, newPiece.pieceRow, newPiece.pieceCol)) {
            EventOrchestrator.publish(GameOver(GameOverReason.BLOCK_OUT, gameSettings.objective.goalType, gameId))
            return null
        }

        currentPiece = newPiece
        canHold = true
        lastAction = LastPieceAction.NONE
        lowestRow = Int.MIN_VALUE
        lockResets = 0
        gameTimers.lockTimer = 0.0

        preserveDas()

        if (holdBuffered && gameSettings.gameplay.isHoldEnabled) {
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
            lastAction = LastPieceAction.MOVE
            gameTimers.lockTimer = gameSettings.gravity.lockDelay
            EventOrchestrator.publish(DefaultGameEvents.HardDrop(distance, gameId))
        }
    }

    override fun softDrop(deltaTime: Double, gravitySpeed: Double) {
        var dropLines = 0
        when (playerSettings.handling.softDropFactor) {
            Double.MAX_VALUE -> {
                Logger.debug { "SOFT_DROP: Factor: ∞x" }
                while (movePiece(1, 0, MoveSource.SOFT_DROP)) {
                    dropLines++
                }
            }

            else -> {
                Logger.debug { "SOFT_DROP: State - Timer: ${gameTimers.softDropTimer}, Delta: $deltaTime, Factor: ${playerSettings.handling.softDropFactor}x" }
                val softDropSpeed = gravitySpeed / playerSettings.handling.softDropFactor
                if (gameTimers.softDropTimer == 0.0) gameTimers.softDropTimer = softDropSpeed
                gameTimers.softDropTimer += deltaTime
                while (gameTimers.softDropTimer >= softDropSpeed) {
                    if (!movePiece(1, 0, MoveSource.SOFT_DROP)) {
                        gameTimers.softDropTimer = 0.0
                        break
                    }
                    dropLines++
                    gameTimers.dropTimer = 0.0
                    gameTimers.softDropTimer -= softDropSpeed
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
            if (source == MoveSource.PLAYER || source == MoveSource.SOFT_DROP) {
                lastAction = LastPieceAction.MOVE
            }
            DefaultGameEvents.PieceMoved(targetRow, targetCol, moving.pieceRow, moving.pieceCol, source, gameId)
            return true
        }
        return false
    }

    override fun rotate(rotation: Rotation): Boolean {
        val piece = currentPiece ?: return false
        if (rotation == Rotation.ROTATE_180 && !gameSettings.gameplay.is180Enabled) return false

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
        if (!gameSettings.gameplay.isHoldEnabled || !canHold || currentPiece == null) {
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
        EventOrchestrator.publish(PieceHeld(heldPiece!!, gameId))
        if (!gameSettings.gameplay.infiniteHold) canHold = false
    }

    override fun clearPiece() {
        currentPiece = null
        clearActionBuffer()
    }

    private fun resetLockTimer() {
        val underCap = lockResets < gameSettings.gravity.maxLockResets
        if (gameSettings.gameplay.infiniteMovement || underCap) {
            gameTimers.lockTimer = 0.0
            if (!gameSettings.gameplay.infiniteMovement) lockResets++
        }
    }

    override fun handleLockDelay(deltaTime: Double, onLock: () -> Unit): Boolean {
        val piece = currentPiece ?: return false

        val isTouchingFloor = !canMove(piece, 1, 0)
        val isInsideBlock = checkCollisionWithBoard(board, piece.shape, piece.pieceRow, piece.pieceCol)

        if (isTouchingFloor || isInsideBlock) {
            gameTimers.lockTimer += deltaTime
            if (gameTimers.lockTimer >= gameSettings.gravity.lockDelay || isInsideBlock) {
                onLock()
                return true
            }
        } else {
            gameTimers.lockTimer = 0.0
        }
        return false
    }

    private fun canMove(piece: MovingPiece, dRow: Int, dCol: Int, row: Int = piece.pieceRow): Boolean {
        return !checkCollisionWithBoard(board, piece.shape, row + dRow, piece.pieceCol + dCol)
    }

    private fun canPlace(piece: MovingPiece, row: Int, col: Int): Boolean {
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
        pieceRandomizer.reset()
        heldPiece = null
        ghostRow = 0
        lowestRow = Int.MIN_VALUE
        lockResets = 0
        dasState = DasState.IDLE
        canHold = true
        lastKickIndex = 0
        clearActionBuffer()
    }

    override fun bufferRotation(rotation: Rotation, isFreshPress: Boolean) {
        when (playerSettings.handling.irsMode) {
            BufferMode.OFF -> return
            BufferMode.TAP -> if (isFreshPress) bufferedRotation = rotation
            BufferMode.HOLD -> bufferedRotation = rotation
        }
    }

    override fun bufferHold(isFreshPress: Boolean) {
        when (playerSettings.handling.ihsMode) {
            BufferMode.OFF -> return
            BufferMode.TAP -> if (isFreshPress) holdBuffered = true
            BufferMode.HOLD -> holdBuffered = true
        }
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

    private fun checkLowestRow(piece: MovingPiece) {
        val currentBottom = piece.pieceRow + piece.shape.rows - 1
        if (currentBottom > lowestRow) {
            lowestRow = currentBottom
            lockResets = 0
            gameTimers.lockTimer = 0.0
            Logger.debug { "New lowest row $lowestRow — lock resets refreshed" }
        }
    }
}