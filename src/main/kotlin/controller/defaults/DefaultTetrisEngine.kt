package controller.defaults

import controller.BoardController
import controller.PieceController
import controller.TetrisEngine
import model.AppLog
import model.BagRandomizer
import model.Command
import model.Drop
import model.GameConfig
import model.GameEvent
import model.GameEventBus
import model.GameGoal
import model.GameSnapshot
import model.GameState
import model.GameTimers
import model.Movement
import model.MovingPiece
import model.Piece
import model.PieceState
import model.Rotation
import model.SpinType
import model.TimeManager
import model.TimeMode
import kotlin.math.floor

abstract class DefaultTetrisEngine<T : Piece>(
    private val settings: GameConfig,
    private val bagManager: BagRandomizer<T>,
    private val gameEventBus: GameEventBus,
    private val gameTimers: GameTimers,
    private var timeManager: TimeManager,
    private val boardManager: BoardController,
    private val pieceController: PieceController<T>,
    override var deltaTime: Float
) : TetrisEngine<T> {

    private var gameState = GameState.ENTRY_DELAY
    private var currentLevel: Int = 1
    private var timeGoalElapsed: Float = 0f
    private var freezeLineClears: Int = 0

    override val isGameOver: Boolean get() = gameState == GameState.GAME_OVER
    override val isVictory: Boolean get() = gameState == GameState.VICTORY

    private val activeDirections = mutableListOf<Int>()
    private val currentDirection: Int? get() = activeDirections.lastOrNull()
    private var rotationLock = false

    init {
        setupTimeSystem()
        setupEventListeners()
    }

    private fun setupTimeSystem() {
        timeManager.onFreezeEnded = {
            val freezeLineClears = 0
            val linesCleared = boardManager.clearFullLines()

            if (linesCleared > 0) {
                gameEventBus.post(GameEvent.LineCleared(SpinType.NONE, linesCleared, boardManager.isBoardEmpty))
            }

            AppLog.info { "Freeze ended. Cleared $linesCleared lines immediately." }
        }
    }

    private fun setupEventListeners() {
        gameEventBus.subscribe<GameEvent.LevelUp> { levelUp() }
    }

    private fun Movement.direction() = when (this) {
        Movement.MOVE_RIGHT -> 1
        Movement.MOVE_LEFT -> -1
    }

    override fun update() {
        if (isGameOver || isVictory) return

        val effectiveDelta = timeManager.tick(deltaTime)

        when (gameState) {
            GameState.ENTRY_DELAY -> {
                gameTimers.areTimer += deltaTime
                if (gameTimers.areTimer >= settings.entryDelay) {
                    gameTimers.areTimer = 0.0f
                    pieceController.spawn(bagManager.getNextPiece())
                    gameState = GameState.PLAYING
                }
            }

            GameState.PLAYING -> {
                gameTimers.sessionTimer += deltaTime
                checkWinCondition()
                pieceController.handleDAS(deltaTime, currentDirection)
                pieceController.handleGravity(currentLevel, effectiveDelta)
                pieceController.handleLockDelay(deltaTime) { lockAndProcess() }
            }

            GameState.GAME_OVER, GameState.VICTORY -> {}
        }
    }

    override fun levelUp(): Int {
        currentLevel = (currentLevel + 1).coerceAtMost(settings.levelCap)
        return currentLevel
    }

    override fun processGarbage(lines: Int, garbageBlockId: Int) {
        boardManager.addGarbage(lines, garbageBlockId)
        gameEventBus.post(GameEvent.GarbageReceived(lines))
    }

    override fun processCommand(command: Command) {
        when (command) {
            Command.HOLD -> pieceController.holdPiece { bagManager.getNextPiece() }
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
        val successfulRotation = pieceController.rotate(rotation)
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

    private fun lockAndProcess() {
        val piece = pieceController.currentPiece ?: return
        val spinType = getSpinType(piece)

        boardManager.placePiece(piece)
        val linesCount = boardManager.getFullLines().size

        if (spinType != SpinType.NONE) gameEventBus.post(GameEvent.SpinDetected(spinType))
        gameEventBus.post(GameEvent.PieceLocked(linesCount > 0))

        if (timeManager.mode == TimeMode.FROZEN) {
            AppLog.info { "Time Frozen" }
            freezeLineClears = floor((freezeLineClears - linesCount).toDouble()).toInt()
            if (freezeLineClears > 0) gameEventBus.post(GameEvent.FreezeLineClear(linesCount, spinType ))
        } else {
            if (linesCount > 0) gameEventBus.post(GameEvent.LineCleared(spinType, linesCount, boardManager.isBoardEmpty))
            boardManager.clearFullLines()
        }

        AppLog.debug { "Piece locked. Cleared $linesCount lines. Spin: $spinType" }
        pieceController.clearPiece()
        gameState = GameState.ENTRY_DELAY
        gameTimers.areTimer = 0.0f
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
        when (settings.goalType) {
            GameGoal.TIME -> {
                timeGoalElapsed += deltaTime
                if (timeGoalElapsed >= settings.goalValue * 1000f) {
                    AppLog.info { "Goal reached! Terminating Game." }
                    gameState = GameState.VICTORY
                    gameEventBus.post(GameEvent.GameOver(true, settings.goalType))
                }
            }

            GameGoal.LINES -> {
                if (boardManager.linesCleared >= settings.goalValue) {
                    AppLog.info { "Goal reached! Terminating Game." }
                    gameState = GameState.VICTORY
                    gameEventBus.post(GameEvent.GameOver(true, settings.goalType))
                }
            }

            else -> {}
        }
    }

}