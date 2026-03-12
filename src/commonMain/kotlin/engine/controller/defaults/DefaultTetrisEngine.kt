package engine.controller.defaults

import engine.controller.BoardController
import engine.controller.PieceController
import engine.controller.TetrisEngine
import engine.model.Board
import engine.model.Drop
import engine.model.FinalKickSpinBehavior
import engine.model.GameGoal
import engine.model.GameOverReason
import engine.model.GameSettings
import engine.model.GameSnapshot
import engine.model.GameState
import engine.model.GameTimers
import engine.model.HUDInfo
import engine.model.LastPieceAction
import engine.model.Movement
import engine.model.MovingPiece
import engine.model.Piece
import engine.model.PieceState
import engine.model.PlayerSettings
import engine.model.Rotation
import engine.model.SpinType
import engine.model.TimeState
import engine.model.defaults.Logger
import engine.model.events.EventOrchestrator
import engine.model.events.GameEvent.FreezeLineClear
import engine.model.events.GameEvent.GameOver
import engine.model.events.GameEvent.LineCleared
import engine.model.events.GameEvent.PieceLocked
import engine.model.events.GameEvent.SpinDetected
import engine.util.addGarbageIfSupported
import engine.util.advanceLockIfSupported
import engine.util.applyGravityIfSupported
import engine.util.bufferHoldIfSupported
import engine.util.bufferRotationIfSupported
import engine.util.clearActionBufferIfSupported
import engine.util.clipIfSupported
import engine.util.collapseIfSupported
import engine.util.getGhostRowIfSupported
import engine.util.getHeldPieceIfSupported
import engine.util.getLastKickIndexIfSupported
import engine.util.getLastKickWasFinalIfSupported
import engine.util.getLockResetsRemainingIfSupported
import engine.util.handleDASIfSupported
import engine.util.hardDropIfSupported
import engine.util.holdIfSupported
import engine.util.resetDASifSupported
import engine.util.softDropIfSupported
import engine.util.tickInputBufferIfSupported
import engine.util.updateGhostIfSupported
import kotlin.math.absoluteValue


abstract class DefaultTetrisEngine<T : Piece>(
    protected val playerSettings: PlayerSettings,
    protected val gameSettings: GameSettings,
    protected val boardManager: BoardController,
    protected val pieceController: PieceController<T>,
    protected val gameTimers: GameTimers = GameTimers(),
    protected val timeManager: TimeManager = TimeManager(),
) : TetrisEngine<T> {
    protected var deltaTime: Double = 0.0
    protected var gameState = GameState.ENTRY_DELAY
    protected var currentLevel: Int = 1
    protected var timeGoalElapsed: Double = 0.0
    protected var freezeLineClears: Int = 0
    override val isGameOver: Boolean get() = gameState == GameState.GAME_OVER
    override val isGoalMet: Boolean get() = gameState == GameState.GOAL_MET
    override val sessionTimeSeconds get() = gameTimers.sessionTimer / 1000.0
    protected val activeDirections = mutableListOf<Int>()
    protected val currentDirection: Int? get() = activeDirections.lastOrNull()
    protected var rotationLock = false
    protected val garbageBuffer = mutableListOf<Int>()
    protected val pendingClearLines = mutableSetOf<Int>()

    protected fun Movement.direction() = when (this) {
        Movement.MOVE_RIGHT -> 1
        Movement.MOVE_LEFT -> -1
    }

    override fun reset() {
        gameState = GameState.ENTRY_DELAY
        currentLevel = 1
        timeGoalElapsed = 0.0
        freezeLineClears = 0
        activeDirections.clear()
        rotationLock = false
        garbageBuffer.clear()

        boardManager.reset()
        pieceController.reset()
        gameTimers.reset()
        timeManager.reset()

        Logger.info { "Engine state reset." }
    }

    open fun update(deltaTime: Double) {
        this.deltaTime = deltaTime
        if (garbageBuffer.isNotEmpty()) {
            processPendingGarbage()
            pieceController.clipIfSupported()
        }
        val gravityDelta = timeManager.tick(deltaTime)
        checkWinCondition()
        pieceController.updateGhostIfSupported()
        when (gameState) {
            GameState.LINE_CLEAR_DELAY -> {
                gameTimers.sessionTimer += deltaTime
                gameTimers.lineClearTimer += deltaTime
                if (gameTimers.lineClearTimer >= playerSettings.lineClearDelay) {
                    pendingClearLines.clear()
                    gameTimers.lineClearTimer = 0.0
                    gameState = GameState.ENTRY_DELAY
                    gameTimers.areTimer = 0.0
                }
            }

            GameState.ENTRY_DELAY -> {
                gameTimers.sessionTimer += deltaTime
                gameTimers.areTimer += deltaTime
                if (gameTimers.areTimer >= playerSettings.entryDelay) {
                    gameTimers.areTimer = 0.0
                    val spawnedPiece = pieceController.spawn()
                    gameState = if (spawnedPiece == null) {
                        pieceController.clearActionBufferIfSupported()
                        GameState.GAME_OVER
                    } else GameState.PLAYING
                }
            }

            GameState.PLAYING -> {
                gameTimers.sessionTimer += deltaTime
                pieceController.tickInputBufferIfSupported(deltaTime)
                pieceController.handleDASIfSupported(deltaTime, currentDirection)
                pieceController.applyGravityIfSupported(gravityDelta)
                pieceController.advanceLockIfSupported(deltaTime, ::lockAndProcess)
            }

            GameState.GAME_OVER -> {}
            GameState.GOAL_MET -> {}
        }
    }

    private fun processPendingGarbage() {
        garbageBuffer.forEach { line ->
            boardManager.addGarbageIfSupported(line, gameSettings.garbageBlockId)
            Logger.info { "Garbage processed: $line for game $gameId" }
        }
        pieceController.clipIfSupported()
        garbageBuffer.clear()
    }

    override fun levelUp(newLevel: Int): Int {
        currentLevel = newLevel
        return currentLevel
    }

    override fun processGarbage(lines: Int) {
        garbageBuffer.add(lines)
    }

    override fun onHold() {
        if (gameState == GameState.ENTRY_DELAY) {
            pieceController.bufferHoldIfSupported()
            return
        }
        pieceController.holdIfSupported()
    }

    override fun gameStateSnapshot(): GameSnapshot<T> {
        val currentPiece = pieceController.currentPiece
        return GameSnapshot(
            boardManager.board,
            currentPiece = currentPiece?.let {
                PieceState(
                    it.shape,
                    it.pieceRow,
                    it.pieceCol,
                    it.piece
                )
            },
            ghostPiece = snapShotGhost(currentPiece),
            nextPieces = pieceController.getNextPieces(playerSettings.previewSize),
            holdPiece = pieceController.getHeldPieceIfSupported(),
            gameState = gameState,
            pendingClearLines = pendingClearLines,
            timeState = timeManager.state,
            timeStateProgress = timeManager.stateProgress,
            hudInfo = HUDInfo(
                combo = ScoreProvider.nullableTracker(gameId)?.combo,
                b2bCount = ScoreProvider.nullableTracker(gameId)?.b2bCount,
                lockResetsRemaining = pieceController.getLockResetsRemainingIfSupported()
            )
        )
    }

    private fun snapShotGhost(currentPiece: MovingPiece<T>?): PieceState<T>? =
        if (playerSettings.isGhostEnabled) pieceController.getGhostRowIfSupported()?.let { ghostRow ->
            currentPiece?.let {
                PieceState(
                    currentPiece.shape, ghostRow, currentPiece.pieceCol, currentPiece.piece
                )
            }
        } else null


    override fun onRotation(rotation: Rotation): Boolean {
        if (rotationLock) return false
        if (gameState == GameState.ENTRY_DELAY) {
            pieceController.bufferRotationIfSupported(rotation)
            return false
        }
        val successfulRotation = pieceController.rotate(rotation)
        val piece = pieceController.currentPiece
        if (piece != null) {
            val spinType = getSpinType(piece)
            if (spinType != SpinType.NONE) EventOrchestrator.publish(SpinDetected(spinType, gameId))
            Logger.debug { "Processing Rotation [$rotation] for piece [${piece.piece.name}]: $successfulRotation | SpinType [$spinType]" }
        }
        if (successfulRotation && playerSettings.dasCutOnRotation) {
            pieceController.resetDASifSupported()
        }
        rotationLock = successfulRotation
        return successfulRotation
    }

    override fun onRotationRelease(rotation: Rotation) {
        rotationLock = false
    }

    override fun onMovement(movement: Movement): Boolean {
        val dir = movement.direction()

        activeDirections.remove(dir)
        activeDirections.add(dir)

        pieceController.resetDASifSupported()
        return pieceController.move(0, dir)
    }

    override fun onMovementRelease(movement: Movement) {
        val previousDirection = currentDirection
        activeDirections.remove(movement.direction())
        val newDirection = currentDirection

        if (newDirection != null && newDirection != previousDirection) {
            pieceController.resetDASifSupported()
            pieceController.move(0, newDirection)
        }
    }

    override fun onDrop(drop: Drop) {
        when (drop) {
            Drop.SOFT_DROP -> pieceController.softDropIfSupported(deltaTime)
            Drop.HARD_DROP -> pieceController.hardDropIfSupported()
        }
    }

    override fun forceBoardState(newState: Board) {
        boardManager.updateBoard(newState)
        pieceController.clearPiece()
        gameState = GameState.ENTRY_DELAY
    }

    override fun onTimeState(timeState: TimeState) {
        timeManager.transition(timeState)
    }

    private fun lockAndProcess() {
        val piece = pieceController.currentPiece ?: return
        pieceController.clipIfSupported()
        boardManager.placePiece(piece)

        val isLockOut = piece.pieceRow + piece.shape.rows - 1 < boardManager.board.bufferSize
        if (isLockOut) {
            gameState = GameState.GAME_OVER
            EventOrchestrator.publish(GameOver(GameOverReason.LOCK_OUT, gameSettings.goalType, gameId))
            return
        }
        Logger.debug { "Piece placed at board: ${piece.pieceRow}, ${piece.pieceCol}" }
        val fullLines = boardManager.getFullLines()
        val linesCount = fullLines.size

        gameState = if (playerSettings.lineClearDelay > 0.0 && linesCount > 0) {
            pendingClearLines.addAll(fullLines)
            GameState.LINE_CLEAR_DELAY
        } else {
            gameTimers.areTimer = 0.0
            GameState.ENTRY_DELAY
        }

        val spinType = getSpinType(piece)
        if (spinType != SpinType.NONE) EventOrchestrator.publish(SpinDetected(spinType, gameId))
        EventOrchestrator.publish(
            PieceLocked(
                fullLines.isNotEmpty(),
                piece.piece,
                piece.pieceRow,
                piece.pieceCol,
                piece.rotationState,
                gameId
            )
        )
        if (timeManager.isFrozen) {
            freezeLineClears = (freezeLineClears - linesCount).absoluteValue
            if (gameSettings.shouldCollapseOnFreeze) boardManager.collapseIfSupported()
            if (freezeLineClears > 0) EventOrchestrator.publish(
                FreezeLineClear(linesCount, spinType, gameId)
            )
        } else {
            boardManager.clearFullLines()
            EventOrchestrator.publish(
                LineCleared(piece.piece, spinType, fullLines, boardManager.isBoardEmpty, gameId)
            )
        }

        Logger.debug { "Piece locked. Cleared $linesCount lines. Spin: $spinType. BoardEmpty: ${boardManager.isBoardEmpty}" }
        pieceController.clearPiece()
    }

    private fun getSpinType(pieceState: MovingPiece<T>): SpinType {
        if (!gameSettings.isSpinEnabled) return SpinType.NONE
        if (pieceController.lastAction != LastPieceAction.ROTATE) return SpinType.NONE

        val kickIndex = pieceController.getLastKickIndexIfSupported()
        val kickWasFinal = pieceController.getLastKickWasFinalIfSupported()

        if (kickWasFinal) {
            return when (playerSettings.finalKickSpinBehavior) {
                FinalKickSpinBehavior.NO_SPIN -> SpinType.NONE
                FinalKickSpinBehavior.ALWAYS_MINI -> SpinType.MINI
                FinalKickSpinBehavior.NORMAL -> {
                    pieceState.piece.getSpinType(
                        boardManager.board,
                        pieceState.pieceRow,
                        pieceState.pieceCol,
                        pieceState.rotationState,
                        kickIndex
                    )
                }
            }
        }

        return pieceState.piece.getSpinType(
            boardManager.board,
            pieceState.pieceRow,
            pieceState.pieceCol,
            pieceState.rotationState,
            kickIndex
        )
    }

    private fun checkWinCondition() {
        if (gameSettings.goalType == GameGoal.TIME) {
            val elapsedSeconds = gameTimers.sessionTimeSeconds

            if (elapsedSeconds >= gameSettings.goalValue) {
                EventOrchestrator.publish(GameOver(GameOverReason.GOAL_MET, gameSettings.goalType, gameId))
                gameState = GameState.GOAL_MET
            }
        }
        if (gameSettings.goalType == GameGoal.LINES) {
            if (boardManager.linesCleared >= gameSettings.goalValue) {
                EventOrchestrator.publish(GameOver(GameOverReason.GOAL_MET, gameSettings.goalType, gameId))
                gameState = GameState.GOAL_MET
            }
        }
    }
}