package engine.controller.defaults

import engine.controller.BoardController
import engine.controller.GameEngine
import engine.controller.PieceController
import engine.controller.defaults.scoring.ScoringEngine
import engine.controller.defaults.scoring.ScoringResult
import engine.model.Board
import engine.model.Drop
import engine.model.EngineTimers
import engine.model.GameGoal
import engine.model.GameOverReason
import engine.model.GameSnapshot
import engine.model.GameState
import engine.model.GameStats
import engine.model.HUDInfo
import engine.model.LastPieceAction
import engine.model.MatchConfig
import engine.model.Movement
import engine.model.MovingPiece
import engine.model.Piece
import engine.model.PieceState
import engine.model.PlayerConfig
import engine.model.Rotation
import engine.model.SpinMode
import engine.model.SpinType
import engine.model.TimeState
import engine.model.events.DefaultGameEvents
import engine.util.Logger
import engine.model.events.DefaultGameEvents.FreezeLineClear
import engine.model.events.DefaultGameEvents.GameOver
import engine.model.events.DefaultGameEvents.LineCleared
import engine.model.events.DefaultGameEvents.PieceLocked
import engine.model.events.DefaultGameEvents.SpinDetected
import engine.model.events.EventOrchestrator
import engine.util.CollisionUtils
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
import engine.util.getLockResetsRemainingIfSupported
import engine.util.handleDASIfSupported
import engine.util.hardDropIfSupported
import engine.util.holdIfSupported
import engine.util.resetDASifSupported
import engine.util.softDropIfSupported
import engine.util.tickInputBufferIfSupported
import engine.util.updateGhostIfSupported
import kotlin.math.absoluteValue


abstract class DefaultGameEngine(
    protected var playerSettings: PlayerConfig,
    protected var gameSettings: MatchConfig,
    protected var boardManager: BoardController,
    protected var pieceController: PieceController,
    protected var scoringEngine: ScoringEngine,
) : GameEngine {
    protected val gameTimers: EngineTimers = EngineTimers()
    protected val timeManager: TimeManager = TimeManager()
    protected val stats = GameStats()
    protected var deltaTime: Double = 0.0
    protected var gameState = GameState.ENTRY_DELAY
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
    protected val gravitySpeed get() = gameSettings.gravity.levelHandler.gravitySpeed(stats.level)
    protected var blockedHardDropFrame = 0

    fun updatePlayerSettings(playerConfig: PlayerConfig) {
        this.playerSettings = playerConfig
    }

    fun updateGameSettings(gameSettings: MatchConfig) {
        this.gameSettings = gameSettings
    }

    fun updatePieceController(pieceController: PieceController) {
        this.pieceController = pieceController
    }


    companion object {
        private const val HARD_DROP_FRAME_GUARD_LIMIT = 2
    }

    protected fun Movement.direction() = when (this) {
        Movement.MOVE_RIGHT -> 1
        Movement.MOVE_LEFT -> -1
    }

    override fun reset() {
        gameState = GameState.ENTRY_DELAY
        timeGoalElapsed = 0.0
        freezeLineClears = 0
        activeDirections.clear()
        rotationLock = false
        garbageBuffer.clear()
        stats.reset()
        boardManager.reset()
        pieceController.reset()
        gameTimers.reset()
        timeManager.reset()
        blockedHardDropFrame = HARD_DROP_FRAME_GUARD_LIMIT
        Logger.info { "Engine state reset." }
    }

    override fun update(deltaTime: Double) {
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
                if (gameTimers.lineClearTimer >= gameSettings.gameplay.lineClearDelay) {
                    pendingClearLines.clear()
                    gameTimers.lineClearTimer = 0.0
                    gameState = GameState.ENTRY_DELAY
                    gameTimers.areTimer = 0.0
                }
            }

            GameState.ENTRY_DELAY -> {
                gameTimers.sessionTimer += deltaTime
                gameTimers.areTimer += deltaTime
                if (gameTimers.areTimer >= gameSettings.gameplay.entryDelay) {
                    gameTimers.areTimer = 0.0
                    val spawnedPiece = pieceController.spawn()
                    blockedHardDropFrame = HARD_DROP_FRAME_GUARD_LIMIT
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
                pieceController.applyGravityIfSupported(gravityDelta, gravitySpeed)
                pieceController.advanceLockIfSupported(deltaTime, ::lockAndProcess)
                if (blockedHardDropFrame > 0) blockedHardDropFrame--
            }

            GameState.GAME_OVER -> {}
            GameState.GOAL_MET -> {}
        }
    }

    private fun processPendingGarbage() {
        garbageBuffer.forEach { line ->
            boardManager.addGarbageIfSupported(line, gameSettings.garbage.garbageBlockId)
            Logger.info { "Garbage processed: $line for game $gameId" }
        }
        pieceController.clipIfSupported()
        garbageBuffer.clear()
    }

    override fun processGarbage(lines: Int) {
        if (lines <= 0) return

        val cap = gameSettings.garbage.cap
        if (cap > 0) {
            val pending = garbageBuffer.sum()
            val allowed = (cap - pending).coerceAtLeast(0)
            if (allowed <= 0) return
            garbageBuffer.add(lines.coerceAtMost(allowed))
        } else {
            garbageBuffer.add(lines)
        }
    }

    override fun onHold(isFreshPress: Boolean) {
        if (gameState == GameState.ENTRY_DELAY) {
            pieceController.bufferHoldIfSupported(isFreshPress)
            return
        }
        pieceController.holdIfSupported()
    }

    override fun gameStateSnapshot(): GameSnapshot {
        val currentPiece = pieceController.currentPiece
        return GameSnapshot(
            boardManager.board,
            currentPiece = currentPiece?.let {
                PieceState(
                    it.shape, it.pieceRow, it.pieceCol, it.piece
                )
            },
            ghostPiece = snapShotGhost(currentPiece),
            nextPieces = pieceController.getNextPieces(gameSettings.gameplay.previewSize),
            holdPiece = pieceController.getHeldPieceIfSupported(),
            gameState = gameState,
            pendingClearLines = pendingClearLines,
            timeState = timeManager.state,
            timeStateProgress = timeManager.stateProgress,
            hudInfo = HUDInfo(
                combo = stats.combo,
                b2bCount = stats.combo,
                lockResetsRemaining = pieceController.getLockResetsRemainingIfSupported(),
                sessionTimeSeconds = sessionTimeSeconds,
                totalLinesCleared = stats.totalLinesCleared,
                currentLevel = stats.level
            )
        )
    }

    private fun snapShotGhost(currentPiece: MovingPiece?): PieceState? =
        if (gameSettings.gameplay.isGhostEnabled) pieceController.getGhostRowIfSupported()?.let { ghostRow ->
            currentPiece?.let {
                PieceState(
                    currentPiece.shape, ghostRow, currentPiece.pieceCol, currentPiece.piece
                )
            }
        } else null


    override fun onRotation(rotation: Rotation, isFreshPress: Boolean): Boolean {
        if (rotationLock) return false
        if (gameState == GameState.ENTRY_DELAY) {
            pieceController.bufferRotationIfSupported(rotation, isFreshPress)
            return false
        }
        val successfulRotation = pieceController.rotate(rotation)
        val piece = pieceController.currentPiece
        if (piece != null) {
            val spinType = getSpinType(piece)
            if (spinType != SpinType.NONE) EventOrchestrator.publish(SpinDetected(spinType, gameId))
            Logger.debug { "Processing Rotation [$rotation] for piece [${piece.piece.name}]: $successfulRotation | SpinType [$spinType]" }
        }
        if (successfulRotation && playerSettings.handling.dasCutOnRotation) {
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

        if (playerSettings.handling.cancelDasOnDirectionChange) {
            pieceController.resetDASifSupported()
        }
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
            Drop.SOFT_DROP -> {
                pieceController.softDropIfSupported(deltaTime, gravitySpeed)
            }

            Drop.HARD_DROP -> {
                if (playerSettings.handling.preventAccidentalHardDrop && blockedHardDropFrame > 0) return
                pieceController.hardDropIfSupported()
            }
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
            EventOrchestrator.publish(GameOver(GameOverReason.LOCK_OUT, gameSettings.objective.goalType, gameId))
            return
        }
        Logger.debug { "Piece placed at board: ${piece.pieceRow}, ${piece.pieceCol}" }
        val fullLines = boardManager.getFullLines()
        val linesCount = fullLines.size

        gameState = if (gameSettings.gameplay.lineClearDelay > 0.0 && linesCount > 0) {
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
                fullLines.isNotEmpty(), piece.piece, piece.pieceRow, piece.pieceCol, piece.rotationState, gameId
            )
        )
        if (timeManager.isFrozen) {
            freezeLineClears = (freezeLineClears - linesCount).absoluteValue
            if (gameSettings.gravity.shouldCollapseOnFreeze) boardManager.collapseIfSupported()
            if (freezeLineClears > 0) EventOrchestrator.publish(
                FreezeLineClear(linesCount, spinType, gameId)
            )
        } else {
            boardManager.clearFullLines()
            EventOrchestrator.publish(
                LineCleared(piece.piece, spinType, fullLines, boardManager.isBoardEmpty, gameId)
            )
        }
        val result = scoringEngine.calculate(spinType, linesCount, stats, boardManager.isBoardEmpty, piece.piece.name)
        EventOrchestrator.publish(
            DefaultGameEvents.ScoreUpdated(
                result.linesCleared, result.pointsAwarded, result.moveType, gameId
            )
        )
        stats.updateFrom(result)
        Logger.debug { stats.toString() }
        checkLevelUp()
        Logger.debug { "Piece locked. Cleared $linesCount lines. Spin: $spinType. BoardEmpty: ${boardManager.isBoardEmpty}" }
        pieceController.clearPiece()
    }

    private fun getSpinType(pieceState: MovingPiece): SpinType {
        val mode = gameSettings.gameplay.spinDetection
        if (mode == SpinMode.NONE) return SpinType.NONE
        if (mode != SpinMode.STUPID && pieceController.lastAction != LastPieceAction.ROTATE) return SpinType.NONE
        if (pieceState.pieceRow < boardManager.board.bufferSize) return SpinType.NONE

        val isEligible = pieceState.piece.isSpinEligible
        val immobile = CollisionUtils.isImmobile(boardManager.board, pieceState)
        val kickIndex = pieceController.getLastKickIndexIfSupported()

        val standardDetection = {
            pieceState.piece.getSpinType(
                boardManager.board, pieceState.pieceRow, pieceState.pieceCol, pieceState.rotationState, kickIndex
            )
        }

        return when (mode) {
            SpinMode.NONE -> SpinType.NONE
            SpinMode.STUPID -> SpinType.FULL
            SpinMode.PIECE_DEFINED -> if (isEligible) standardDetection() else SpinType.NONE
            SpinMode.PIECE_DEFINED_PLUS -> when {
                !isEligible -> SpinType.NONE
                immobile -> SpinType.FULL
                else -> standardDetection()
            }

            SpinMode.ALL -> standardDetection()
            SpinMode.ALL_PLUS -> if (immobile) SpinType.FULL else standardDetection()
            SpinMode.ALL_MINI -> when {
                isEligible -> standardDetection()
                else -> if (standardDetection() != SpinType.NONE) SpinType.MINI else SpinType.NONE
            }

            SpinMode.ALL_MINI_PLUS -> when {
                isEligible && immobile -> SpinType.FULL
                isEligible -> standardDetection()
                immobile -> SpinType.MINI
                else -> if (standardDetection() != SpinType.NONE) SpinType.MINI else SpinType.NONE
            }
        }
    }

    private fun checkLevelUp() {
        val newLevel = gameSettings.gravity.levelHandler.levelForLines(stats.totalLinesCleared, stats.level)
        if (newLevel > stats.level) {
            stats.level = newLevel
            EventOrchestrator.publish(DefaultGameEvents.LevelUp(newLevel, gameId))
        }
    }

    private fun checkWinCondition() {
        if (gameSettings.objective.goalType == GameGoal.TIME) {
            val elapsedSeconds = gameTimers.sessionTimeSeconds

            if (elapsedSeconds >= gameSettings.objective.goalValue) {
                EventOrchestrator.publish(GameOver(GameOverReason.GOAL_MET, gameSettings.objective.goalType, gameId))
                gameState = GameState.GOAL_MET
            }
        }
        if (gameSettings.objective.goalType == GameGoal.LINES) {
            if (boardManager.linesCleared >= gameSettings.objective.goalValue) {
                EventOrchestrator.publish(GameOver(GameOverReason.GOAL_MET, gameSettings.objective.goalType, gameId))
                gameState = GameState.GOAL_MET
            }
        }
    }
}

private fun GameStats.updateFrom(result: ScoringResult) {
    this.combo = result.newCombo
    this.b2bCount = result.newB2b
    this.totalLinesCleared += result.linesCleared
}
