package engine.controller.defaults

import engine.controller.BagRandomizer
import engine.controller.BoardController
import engine.controller.PieceController
import engine.controller.TetrisEngine
import engine.model.Board
import engine.model.Command
import engine.model.Drop
import engine.model.GameGoal
import engine.model.GameSettings
import engine.model.GameSnapshot
import engine.model.GameState
import engine.model.GameTimers
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
import engine.model.events.Event
import engine.model.events.EventOrchestrator
import engine.model.events.GameEvent.FreezeLineClear
import engine.model.events.GameEvent.GameOver
import engine.model.events.GameEvent.GarbageReceived
import engine.model.events.GameEvent.LevelUp
import engine.model.events.GameEvent.LineCleared
import engine.model.events.GameEvent.PieceLocked
import engine.model.events.GameEvent.SpinDetected
import engine.model.events.InputEvent.CommandInput
import engine.model.events.InputEvent.DirectionMoveEnd
import engine.model.events.InputEvent.DirectionMoveStart
import engine.model.events.InputEvent.DropInput
import engine.model.events.InputEvent.FreezeTime
import engine.model.events.InputEvent.RotationInputRelease
import engine.model.events.InputEvent.RotationInputStart
import engine.model.events.InputEvent.SlowDownTime
import kotlin.math.absoluteValue


abstract class DefaultTetrisEngine<T : Piece>(
    protected val playerSettings: PlayerSettings,
    protected val gameSettings: GameSettings,
    protected val bagManager: BagRandomizer<T>,
    protected val boardManager: BoardController,
    protected val pieceController: PieceController<T>,
    protected val gameTimers: GameTimers = GameTimers(),
    protected val timeManager: TimeManager = TimeManager(gameSettings),
) : TetrisEngine<T> {
    private var deltaTime: Double = 0.0
    private var gameState = GameState.ENTRY_DELAY
    private var currentLevel: Int = 1
    private var timeGoalElapsed: Double = 0.0
    private var freezeLineClears: Int = 0

    override val isGameOver: Boolean get() = gameState == GameState.GAME_OVER
    override val isGoalMet: Boolean get() = gameState == GameState.GOAL_MET
    override val sessionTimeSeconds get() = gameTimers.sessionTimer / 1000.0
    private val activeDirections = mutableListOf<Int>()
    private val currentDirection: Int? get() = activeDirections.lastOrNull()
    private var rotationLock = false
    private val garbageBuffer = mutableListOf<Int>()

    init {
        setupTimeSystem()
        setupInputEvents()
        setupGameEvents()
    }

    private fun setupTimeSystem() {
        timeManager.onFreezeEnded = {
            freezeLineClears = 0
            val linesCleared = boardManager.clearFullLines()

            if (linesCleared.isNotEmpty()) {
                EventOrchestrator.publish(
                    LineCleared(SpinType.NONE, linesCleared, boardManager.isBoardEmpty, gameId)
                )
            }
            Logger.info { "Freeze ended. Cleared $linesCleared lines immediately." }
        }
    }

    private fun setupGameEvents() {
        subscribeForGame<LevelUp, Int>(::levelUp) { it.newLevel }
        subscribeForGame<GarbageReceived, Int>({ lines ->
            processGarbage(lines)
        }, {
            it.lines
        })
    }

    private fun setupInputEvents() {
        subscribeForGame<CommandInput, Command>(::onCommand) { it.command }
        subscribeForGame<DirectionMoveStart, Movement>(::onMovement) { it.movement }
        subscribeForGame<DirectionMoveEnd, Movement>(::onMovementRelease) { it.movement }
        subscribeForGame<DropInput, Drop>(::onDrop) { it.dropType }
        subscribeForGame<RotationInputStart, Rotation>(::onRotation) { it.rotation }
        subscribeForGame<RotationInputRelease, Rotation>(::onRotationRelease) { it.rotation }
        subscribeForGame<SlowDownTime, Double>(
            { duration -> onTimeState(TimeState.SLOWED, duration) },
            { it.duration }
        )
        subscribeForGame<FreezeTime, Double>(
            { duration -> onTimeState(TimeState.FROZEN, duration) },
            { it.duration }
        )
    }

    private fun Movement.direction() = when (this) {
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
        bagManager.reset()
        gameTimers.reset()
        timeManager.reset()

        Logger.info { "Engine state reset." }
    }

    open suspend fun update(deltaTime: Double) {
        this.deltaTime = deltaTime
        if (garbageBuffer.isNotEmpty()) {
            processPendingGarbage()
            pieceController.clip()
        }
        val gravityDelta = timeManager.tick(deltaTime)
        checkWinCondition()
        pieceController.updateGhost()
        when (gameState) {
            GameState.ENTRY_DELAY -> {
                gameTimers.sessionTimer += deltaTime
                gameTimers.areTimer += deltaTime
                if (gameTimers.areTimer >= playerSettings.entryDelay) {
                    gameTimers.areTimer = 0.0
                    val spawnedPiece = pieceController.spawn(bagManager.getNextPiece())
                    gameState = if (spawnedPiece == null) {
                        EventOrchestrator.publish(GameOver(false, gameSettings.goalType, gameId))
                        GameState.GAME_OVER
                    } else GameState.PLAYING
                }
            }

            GameState.PLAYING -> {
                gameTimers.sessionTimer += deltaTime
                pieceController.handleDAS(deltaTime, currentDirection)
                pieceController.handleGravity(currentLevel, gravityDelta)
                pieceController.handleLockDelay(deltaTime, ::lockAndProcess)
            }

            GameState.GAME_OVER -> {}
            GameState.GOAL_MET -> {}
        }
    }

    private suspend fun processPendingGarbage() {
        garbageBuffer.forEach { line ->
            boardManager.addGarbage(line, gameSettings.garbageBlockId)
            Logger.info { "Garbage processed: $line for game $gameId" }
        }
        pieceController.clip()
        garbageBuffer.clear()
    }

    override suspend fun levelUp(newLevel: Int): Int {
        currentLevel = newLevel
        return currentLevel
    }

    override suspend fun processGarbage(lines: Int) {
        garbageBuffer.add(lines)
    }

    override suspend fun onCommand(command: Command) {
        when (command) {
            Command.HOLD -> pieceController.holdPiece { bagManager.getNextPiece() }
            Command.RESET -> reset()
        }
    }

    override suspend fun gameStateSnapshot(): GameSnapshot<T> {
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
            ghostPiece = if (playerSettings.isGhostEnabled) pieceController.currentPiece?.let {
                PieceState(
                    it.shape, pieceController.ghostRow, it.pieceCol, it.piece
                )
            } else null,
            nextPieces = bagManager.getPreview(playerSettings.previewSize),
            holdPiece = pieceController.heldPiece)
    }


    override suspend fun onRotation(rotation: Rotation): Boolean {
        if (rotationLock) return false
        val successfulRotation = pieceController.rotate(rotation)
        val piece = pieceController.currentPiece
        if (piece != null) {
            val spinType = getSpinType(piece)
            if (spinType != SpinType.NONE) EventOrchestrator.publish(SpinDetected(spinType, gameId))
            Logger.debug { "Processing Rotation [$rotation] for piece [${pieceController.currentPiece?.piece?.name}]: $successfulRotation | SpinType [$spinType]" }
        }
        rotationLock = successfulRotation
        return successfulRotation
    }

    override suspend fun onRotationRelease(rotation: Rotation) {
        rotationLock = false
    }

    override suspend fun onMovement(movement: Movement): Boolean {
        val dir = movement.direction()

        activeDirections.remove(dir)
        activeDirections.add(dir)

        val successfulMovement = pieceController.move(0, dir)
        if (successfulMovement) {
            pieceController.resetDas()
        }
        return successfulMovement
    }

    override suspend fun onMovementRelease(movement: Movement) {
        activeDirections.remove(movement.direction())
    }

    override suspend fun onDrop(drop: Drop) {
        when (drop) {
            Drop.SOFT_DROP -> pieceController.softDrop(deltaTime)
            Drop.HARD_DROP -> pieceController.hardDrop()
        }
    }

    override suspend fun forceBoardState(newState: Board) {
        boardManager.updateBoard(newState)
        pieceController.clearPiece()
        gameState = GameState.ENTRY_DELAY
    }

    override suspend fun onTimeState(timeState: TimeState, duration: Double) {
        timeManager.onState(timeState, duration)
    }

    private suspend fun lockAndProcess() {
        val piece = pieceController.currentPiece ?: return
        pieceController.clip()
        boardManager.placePiece(piece)
        Logger.debug { "Piece placed at board: ${piece.pieceRow}, ${piece.pieceCol}" }
        val fullLines = boardManager.getFullLines()
        val linesCount = fullLines.size

        val spinType = getSpinType(piece)
        if (spinType != SpinType.NONE) EventOrchestrator.publish(SpinDetected(spinType, gameId))
        EventOrchestrator.publish(PieceLocked(linesCount > 0, gameId))

        if (timeManager.mode == TimeState.FROZEN) {
            freezeLineClears = (freezeLineClears - linesCount).absoluteValue
            if (gameSettings.shouldCollapseOnFreeze) boardManager.collapseFullLines()
            if (freezeLineClears > 0) EventOrchestrator.publish(
                FreezeLineClear(linesCount, spinType, gameId)
            )
        } else {
            boardManager.clearFullLines()
            EventOrchestrator.publish(
                LineCleared(
                    spinType,
                    fullLines,
                    boardManager.isBoardEmpty, gameId
                )
            )
        }

        Logger.debug { "Piece locked. Cleared $linesCount lines. Spin: $spinType. BoardEmpty: ${boardManager.isBoardEmpty}" }
        pieceController.clearPiece()
        gameState = GameState.ENTRY_DELAY
        gameTimers.areTimer = 0.0
    }

    private fun getSpinType(pieceState: MovingPiece<T>): SpinType {
        if (!playerSettings.isSpinEnabled || pieceController.lastAction != LastPieceAction.ROTATE) return SpinType.NONE
        return pieceState.piece.getSpinType(
            boardManager.board,
            pieceState.pieceRow,
            pieceState.pieceCol,
            pieceState.rotationState
        )
    }

    private fun checkWinCondition() {
        if (gameSettings.goalType == GameGoal.TIME) {
            val elapsedSeconds = gameTimers.sessionTimeSeconds

            if (elapsedSeconds >= gameSettings.goalValue) {
                EventOrchestrator.publish(GameOver(true, gameSettings.goalType, gameId))
                gameState = GameState.GOAL_MET
            }
        }
        if (gameSettings.goalType == GameGoal.LINES) {
            if (boardManager.linesCleared >= gameSettings.goalValue) {
                EventOrchestrator.publish(GameOver(true, gameSettings.goalType, gameId))
                gameState = GameState.GOAL_MET
            }
        }
    }

    private inline fun <reified E : Event, V> subscribeForGame(
        crossinline handler: suspend (V) -> Unit,
        crossinline extractor: (E) -> V
    ) {
        EventOrchestrator.subscribe<E, V>(
            { value ->
                if (value != null) {
                    handler(value)
                }
            },
            { event ->
                if (event.gameId == gameId) extractor(event) else null
            }
        )
    }
}