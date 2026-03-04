package controller.defaults

import controller.BoardController
import controller.PieceController
import controller.TetrisEngine
import model.AppLog
import model.BagRandomizer
import model.Board
import model.Command
import model.Drop
import model.GameGoal
import model.GameSettings
import model.GameSnapshot
import model.GameState
import model.GameTimers
import model.Movement
import model.MovingPiece
import model.Piece
import model.PieceState
import model.Rotation
import model.SpinType
import model.TimeMode
import model.events.EventHandler
import model.events.GameEvent.FreezeLineClear
import model.events.GameEvent.GameOver
import model.events.GameEvent.GarbageReceived
import model.events.GameEvent.LevelUp
import model.events.GameEvent.LineCleared
import model.events.GameEvent.PieceLocked
import model.events.GameEvent.SpinDetected
import kotlin.math.absoluteValue


abstract class DefaultTetrisEngine<T : Piece>(
    private val settings: GameSettings,
    private val bagManager: BagRandomizer<T>,
    private val gameTimers: GameTimers,
    private var timeManager: TimeManager,
    private val boardManager: BoardController,
    private val pieceController: PieceController<T>,
) : TetrisEngine<T> {

    private var deltaTime: Float = 0f
    private var gameState = GameState.ENTRY_DELAY
    private var currentLevel: Int = 1
    private var timeGoalElapsed: Float = 0f
    private var freezeLineClears: Int = 0

    override val isGameOver: Boolean get() = gameState == GameState.GAME_OVER
    override val isGoalMet: Boolean get() = gameState == GameState.GOAL_MET
    override val sessionTimeSeconds get() = gameTimers.sessionTimer / 1000f
    private val activeDirections = mutableListOf<Int>()
    private val currentDirection: Int? get() = activeDirections.lastOrNull()
    private var rotationLock = false

    init {
        setupTimeSystem()
        setupEventListeners()
    }

    private fun setupTimeSystem() {
        timeManager.onFreezeEnded = {
            freezeLineClears = 0
            val linesCleared = boardManager.clearFullLines()

            if (linesCleared > 0) {
                EventHandler.publish(
                    LineCleared.topic,
                    LineCleared(SpinType.NONE, linesCleared, boardManager.isBoardEmpty)
                )
            }
            AppLog.info { "Freeze ended. Cleared $linesCleared lines immediately." }
        }
    }

    private fun setupEventListeners() {
        EventHandler.subscribeToEvent<LevelUp> {
            levelUp()
        }
    }

    private fun Movement.direction() = when (this) {
        Movement.MOVE_RIGHT -> 1
        Movement.MOVE_LEFT -> -1
    }

    override fun reset() {
        gameState = GameState.ENTRY_DELAY
        currentLevel = 1
        timeGoalElapsed = 0f
        freezeLineClears = 0
        activeDirections.clear()
        rotationLock = false


        boardManager.reset()
        pieceController.reset()
        bagManager.reset()
        gameTimers.reset()
        timeManager.reset()

        AppLog.info { "Engine state reset." }
    }

    open fun update(deltaTime: Float) {
        this.deltaTime = deltaTime
        val gravityDelta = timeManager.tick(deltaTime)
        checkWinCondition()
        when (gameState) {
            GameState.ENTRY_DELAY -> {
                gameTimers.sessionTimer += deltaTime
                gameTimers.areTimer += deltaTime
                if (gameTimers.areTimer >= settings.entryDelay) {
                    gameTimers.areTimer = 0f
                    val spawnedPiece = pieceController.spawn(bagManager.getNextPiece())
                    gameState = if (spawnedPiece == null) {
                        EventHandler.publish(GameOver.topic, GameOver(false, settings.goalType))
                        GameState.GAME_OVER
                    } else GameState.PLAYING
                }
            }

            GameState.PLAYING -> {
                gameTimers.sessionTimer += deltaTime
                pieceController.handleDAS(deltaTime, currentDirection)
                pieceController.handleGravity(currentLevel, gravityDelta)
                pieceController.handleLockDelay(deltaTime) { lockAndProcess() }
            }

            GameState.GAME_OVER -> {}
            GameState.GOAL_MET -> {}
        }
    }

    override fun levelUp(): Int {
        currentLevel = (currentLevel + 1).coerceAtMost(settings.levelCap)
        return currentLevel
    }

    override fun processGarbage(lines: Int, garbageBlockId: Int) {
        boardManager.addGarbage(lines, garbageBlockId)
        AppLog.info { "Garbage processed: $lines" }
        EventHandler.publish(GarbageReceived.topic, GarbageReceived(lines))
    }

    override fun processCommand(command: Command) {
        when (command) {
            Command.HOLD -> pieceController.holdPiece { bagManager.getNextPiece() }
            else -> reset()
        }
    }

    override fun gameStateSnapshot(): GameSnapshot<T> {
        return GameSnapshot(
            boardManager.board,
            currentPiece = pieceController.currentPiece?.let {
                PieceState(
                    it.shape,
                    it.pieceRow,
                    it.pieceCol,
                    it.piece
                )
            },
            ghostPiece = if (settings.isGhostEnabled) pieceController.currentPiece?.let {
                PieceState(
                    it.shape, pieceController.ghostRow, it.pieceCol, it.piece
                )
            } else null,
            nextPieces = bagManager.getPreview(settings.previewSize),
            holdPiece = pieceController.heldPiece)
    }


    override fun onRotation(rotation: Rotation): Boolean {
        if (rotationLock) return false
        val successfulRotation = pieceController.rotate(rotation)
        val piece = pieceController.currentPiece
        if (piece != null) {
            val spinType = getSpinType(piece)
            if (spinType != SpinType.NONE) EventHandler.publish(SpinDetected.topic, SpinDetected(spinType))
            AppLog.debug { "Processing Rotation [$rotation] for piece [${pieceController.currentPiece?.piece?.name}]: $successfulRotation | SpinType [$spinType]" }
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

        val successfulMovement = pieceController.move(0, dir)
        if (successfulMovement) {
            pieceController.resetDas()
        }
        return successfulMovement
    }

    override fun onMovementRelease(movement: Movement) {
        activeDirections.remove(movement.direction())
    }

    override fun processDrop(drop: Drop) {
        when (drop) {
            Drop.SOFT_DROP -> pieceController.softDrop(deltaTime)
            Drop.HARD_DROP -> pieceController.hardDrop()
        }
    }

    override fun forceBoardState(newState: Board) {
        boardManager.updateBoard(newState)
        pieceController.clearPiece()
        gameState = GameState.ENTRY_DELAY
    }

    private fun lockAndProcess() {
        val piece = pieceController.currentPiece ?: return


        boardManager.placePiece(piece)
        val linesCount = boardManager.getFullLines().size

        val spinType = getSpinType(piece)
        if (spinType != SpinType.NONE) EventHandler.publish(SpinDetected.topic, SpinDetected(spinType))
        EventHandler.publish(PieceLocked.topic, PieceLocked(linesCount > 0))

        if (timeManager.mode == TimeMode.FROZEN) {
            freezeLineClears = (freezeLineClears - linesCount).absoluteValue
            if (settings.shouldCollapseOnFreeze) boardManager.collapseFullLines()
            if (freezeLineClears > 0) EventHandler.publish(
                FreezeLineClear.topic,
                FreezeLineClear(linesCount, spinType)
            )
        } else {
            boardManager.clearFullLines()
            EventHandler.publish(
                LineCleared.topic,
                LineCleared(
                    spinType,
                    linesCount,
                    boardManager.isBoardEmpty
                )
            )
        }

        AppLog.debug { "Piece locked. Cleared $linesCount lines. Spin: $spinType. BoardEmpty: ${boardManager.isBoardEmpty}" }
        pieceController.clearPiece()
        gameState = GameState.ENTRY_DELAY
        gameTimers.areTimer = 0f
    }

    private fun getSpinType(pieceState: MovingPiece<T>): SpinType {
        if (!settings.isSpinEnabled || !pieceController.wasRotated) return SpinType.NONE
        return pieceState.piece.getSpinType(
            boardManager.board,
            pieceState.pieceRow,
            pieceState.pieceCol,
            pieceState.rotationState
        )
    }

    private fun checkWinCondition() {
        if (settings.goalType == GameGoal.TIME) {
            val elapsedSeconds = gameTimers.sessionTimer / 1000f

            if (elapsedSeconds >= settings.goalValue) {
                EventHandler.publish(GameOver.topic, GameOver(true, settings.goalType))
                gameState = GameState.GOAL_MET
            }
        }

        if (settings.goalType == GameGoal.LINES) {
            if (boardManager.linesCleared >= settings.goalValue) {
                EventHandler.publish(GameOver.topic, GameOver(true, settings.goalType))
                gameState = GameState.GOAL_MET
            }
        }
    }

}