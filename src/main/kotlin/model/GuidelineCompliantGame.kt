package model

import util.CollisionUtils.checkCollision

enum class GameState { PLAYING, ENTRY_DELAY, GAME_OVER, VICTORY }

enum class DasState { IDLE, DELAY, REPEAT }

class BaseTetris<T : Piece>(
    settings: GameConfig,
    bagManager: BagRandomizer<T>,
    gameEventBus: GameEventBus,
    commandRecorder: CommandRecorder? = null,
) : GuidelineCompliantGame<T>(
    settings, bagManager, gameEventBus, commandRecorder, 0f
)

abstract class GuidelineCompliantGame<T : Piece>(
    private val settings: GameConfig,
    private val bagManager: BagRandomizer<T>,
    private val gameEventBus: GameEventBus,
    private val commandRecorder: CommandRecorder? = null,
    override var deltaTime: Float
) : TetrisEngine<T> {

    companion object {
        private const val SOFT_DROP_PRECISION_EPSILON = 0.001f
    }

    private val gameTimers: GameTimers = GameTimers()
    private val board: Matrix<Int> = Matrix(settings.boardRows, settings.boardCols, 0)

    private var currentPiece: MovingPiece<T>? = null
    private var ghostRow: Int = 0
    private var heldPiece: T? = null
    private var canHold: Boolean = true
    private var wasRotated: Boolean = false
    private var gameState = GameState.ENTRY_DELAY
    private var currentLevel: Int = 1
    private var lockResets: Int = 0
    private var timeGoalElapsed: Float = 0f
    private var dasState: DasState = DasState.IDLE
    private var dasTimer: Float = 0f
    private var totalLinesCleared = 0

    override val isGameOver: Boolean get() = gameState == GameState.GAME_OVER
    override val isVictory: Boolean get() = gameState == GameState.VICTORY
    private val isBoardEmpty: Boolean get() = board.isEmpty(0)

    private val activeDirections = mutableListOf<Int>()
    private val currentDirection: Int? get() = activeDirections.lastOrNull()
    private var rotationLock = false

    init {
        gameEventBus.subscribe<GameEvent.LevelUp> {
            levelUp()
        }

        gameEventBus.subscribe<InputEvent> { event ->
            if (isGameOver || isVictory) return@subscribe
            when (event) {
                is InputEvent.DirectionMoveStart -> {
                    val dir = event.movement.direction()

                    activeDirections.remove(dir)
                    activeDirections.add(dir)

                    if (movePiece(0, dir)) {
                        dasState = DasState.DELAY
                        dasTimer = 0f
                    }
                }

                is InputEvent.DirectionMoveEnd -> {
                    activeDirections.remove(event.movement.direction())
                }

                is InputEvent.DropInput -> processDrop(event.dropType)
                is InputEvent.CommandInput -> holdPiece()
                is InputEvent.RotationInputStart -> {
                    processRotation(event.rotation)
                    rotationLock = true
                }

                is InputEvent.RotationInputRelease -> rotationLock = false
            }
            commandRecorder?.record(event, deltaTime)
        }
    }

    private fun Movement.direction() = when (this) {
        Movement.MOVE_RIGHT -> 1
        Movement.MOVE_LEFT -> -1
    }

    override fun update() {
        if (isGameOver || isVictory) return
        when (gameState) {
            GameState.ENTRY_DELAY -> {
                gameTimers.areTimer += deltaTime
                if (gameTimers.areTimer >= settings.entryDelay) {
                    gameTimers.areTimer = 0.0f
                    spawnPiece(bagManager.getNextPiece())
                    gameState = GameState.PLAYING
                }
            }

            GameState.PLAYING -> {
                gameTimers.sessionTimer += deltaTime
                updateGhost()
                checkWinCondition()
                handleDAS()
                handleGravity()
                handleLockDelay()
            }

            GameState.GAME_OVER, GameState.VICTORY -> {}
        }
    }

    override fun spawnPiece(nextPiece: T) {
        if (isGameOver) return

        AppLog.debug { "Spawning piece: $nextPiece" }
        val newPiece = MovingPiece(
            piece = nextPiece, pieceCol = (board.cols / 2) - (nextPiece.shape.cols / 2)
        )

        if (checkCollision(board, newPiece.shape, newPiece.pieceRow, newPiece.pieceCol)) {
            gameState = GameState.GAME_OVER
            gameEventBus.post(GameEvent.GameOver(false, settings.goalType))
            return
        }

        currentPiece = newPiece
        canHold = true
        wasRotated = false
        lockResets = 0
        gameTimers.lockTimer = 0.0f

        updateGhost()
        gameEventBus.post(GameEvent.NewPiece(newPiece.piece))
    }

    override fun levelUp(): Int {
        currentLevel = (currentLevel + 1).coerceAtMost(settings.levelCap)
        return currentLevel
    }

    override fun processGarbage(lines: Int, garbageBlockId: Int) {
        if (lines <= 0) return

        val holeCol = (0 until board.cols).random()

        repeat(lines) {
            for (r in 0 until board.rows - 1) {
                for (c in 0 until board.cols) {
                    board[r, c] = board[r + 1, c]
                }
            }
            for (c in 0 until board.cols) {
                board[board.rows - 1, c] = if (c == holeCol) 0 else garbageBlockId
            }
        }

        updateGhost()
        gameEventBus.post(GameEvent.GarbageReceived(lines))

        if (!board.isEmpty(0)) {
            gameState = GameState.GAME_OVER
            gameEventBus.post(GameEvent.GameOver(false, settings.goalType))
        }
    }

    override fun processCommand(command: Command) {
        when (command) {
            Command.HOLD -> holdPiece()
        }
    }

    private fun holdPiece() {
        if (!settings.isHoldEnabled || !canHold || currentPiece == null) return

        val pieceToHold = currentPiece!!.piece
        if (heldPiece == null) {
            heldPiece = pieceToHold
            spawnPiece(bagManager.getNextPiece())
        } else {
            val next = heldPiece!!
            heldPiece = pieceToHold
            spawnPiece(next)
        }
        AppLog.info { "Piece held: $heldPiece" }
        gameEventBus.post(GameEvent.PieceHeld(heldPiece!!))
        canHold = false
    }

    override fun gameStateSnapshot(): GameSnapshot<T> {
        return GameSnapshot(
            board,
            currentPiece = currentPiece?.let { PieceState(it.shape, it.pieceRow, it.pieceCol, it.piece) },
            ghostPiece = if (settings.isGhostEnabled) currentPiece?.let {
                PieceState(
                    it.shape, ghostRow, it.pieceCol, it.piece
                )
            } else null,
            nextPieces = bagManager.getPreview(settings.previewSize),
            holdPiece = heldPiece)
    }

    private fun handleDAS() {
        val dir = currentDirection ?: return
        dasTimer += deltaTime

        when (dasState) {
            DasState.IDLE -> return

            DasState.DELAY -> {
                if (dasTimer >= settings.dasDelay) {
                    dasState = DasState.REPEAT
                    dasTimer -= settings.dasDelay
                }
            }

            DasState.REPEAT -> {
                while (dasTimer >= settings.arrDelay) {
                    if (!movePiece(0, dir)) {
                        dasTimer = 0f
                        break
                    }
                    dasTimer -= settings.arrDelay
                }
            }
        }
    }

    private fun resetLockTimer() {
        if (lockResets < settings.maxLockResets) {
            gameTimers.lockTimer = 0.0f
            lockResets++
        }
    }

    private fun hardDrop() {
        currentPiece?.let { piece ->
            val distance = ghostRow - piece.pieceRow
            piece.pieceRow = ghostRow
            gameTimers.lockTimer = settings.lockDelay
            gameEventBus.post(GameEvent.HardDrop(distance))
        }
    }

    private fun softDrop() {
        gameTimers.softDropTimer += deltaTime
        var dropLines = 0
        if (settings.softDropDelay <= SOFT_DROP_PRECISION_EPSILON) {
            while (movePiece(1, 0)) {
                dropLines++
                gameTimers.dropTimer = 0.0f
                wasRotated = false
            }
        } else {
            while (gameTimers.softDropTimer >= settings.softDropDelay) {
                if (movePiece(1, 0)) {
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

    override fun processRotation(rotation: Rotation): Boolean {
        val moving = currentPiece ?: return false
        if (rotation == Rotation.ROTATE_180 && !settings.is180Enabled) return false

        val (candidateShape, _) = moving.projectRotation(rotation)
        val tests = moving.piece.getKickTable(rotation, moving.rotationState)

        for (index in tests.indices) {
            val (offsetX, offsetY) = tests[index]
            val testCol = moving.pieceCol + offsetX
            val testRow = moving.pieceRow - offsetY
            val isCollision = checkCollision(board, candidateShape, testRow, testCol)
            if (!isCollision) {
                moving.rotateShape(candidateShape, testRow, testCol, rotation)
                gameTimers.lockTimer = 0.0f
                resetLockTimer()
                wasRotated = true
                gameEventBus.post(GameEvent.PieceRotated(moving.piece, moving.rotationState))
                return true
            }
        }
        return false
    }

    override fun processMove(movement: Movement): Boolean {
        val piece = currentPiece ?: return false
        val horizontalDirection = when (movement) {
            Movement.MOVE_RIGHT -> 1
            Movement.MOVE_LEFT -> -1
        }
        return movePiece(piece.pieceRow, piece.pieceCol + horizontalDirection)
    }

    override fun processDrop(drop: Drop) {
        when (drop) {
            Drop.SOFT_DROP -> softDrop()
            Drop.HARD_DROP -> hardDrop()
        }
    }

    private fun lockAndProcess() {
        val piece = currentPiece ?: return
        val spinType = getSpinType(piece)

        val shape = piece.shape
        for (r in 0 until shape.rows) {
            for (c in 0 until shape.cols) {
                if (shape[r, c] != 0) {
                    board[piece.pieceRow + r, piece.pieceCol + c] = shape[r, c]
                }
            }
        }

        val linesCleared = clearLines()
        totalLinesCleared += linesCleared
        currentPiece = null
        gameState = GameState.ENTRY_DELAY
        gameTimers.areTimer = 0.0f
        AppLog.debug { "Piece locked. Cleared $linesCleared lines. Spin: $spinType" }
        if (spinType != SpinType.NONE) gameEventBus.post(GameEvent.SpinDetected(spinType))
        if (linesCleared > 0) gameEventBus.post(GameEvent.LineCleared(spinType, linesCleared, isBoardEmpty))
        gameEventBus.post(GameEvent.PieceLocked(linesCleared > 0))
    }

    private fun getSpinType(pieceState: MovingPiece<T>): SpinType {
        if (!settings.isSpinEnabled || !wasRotated) return SpinType.NONE
        return pieceState.piece.getSpinType(
            board,
            pieceState.pieceRow,
            pieceState.pieceCol,
            pieceState.rotationState
        )
    }

    private fun clearLines(): Int {
        var linesCleared = 0
        for (r in 0 until board.rows) {
            var fullLine = true
            for (c in 0 until board.cols) {
                if (board[r, c] == 0) {
                    fullLine = false
                    break
                }
            }
            if (fullLine) {
                for (row in r downTo 1) for (col in 0 until board.cols) board[row, col] = board[row - 1, col]
                for (col in 0 until board.cols) board[0, col] = 0
                linesCleared++
            }
        }
        if (linesCleared > 0) {
            checkWinCondition()
        }
        return linesCleared
    }

    private fun handleGravity() {
        val gravitySpeed = settings.gravityBase - (currentLevel - 1) * settings.gravityIncrement

        val effectiveGravity = gravitySpeed.coerceAtLeast(10f)

        gameTimers.dropTimer += deltaTime

        if (gameTimers.dropTimer >= effectiveGravity) {
            if (movePiece(1, 0)) {
                wasRotated = false
                gameTimers.lockTimer = 0.0f
                gameTimers.dropTimer -= effectiveGravity
            } else {
                gameTimers.dropTimer = 0.0f
            }
        }
    }

    private fun handleLockDelay() {
        if (currentPiece != null && !canMove(currentPiece!!, 1, 0)) {
            gameTimers.lockTimer += deltaTime
            if (gameTimers.lockTimer >= settings.lockDelay) {
                lockAndProcess()
            }
        } else {
            gameTimers.lockTimer = 0.0f
        }
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

    private fun canMove(piece: MovingPiece<T>, dRow: Int, dCol: Int, row: Int = piece.pieceRow): Boolean {
        return !checkCollision(board, piece.shape, row + dRow, piece.pieceCol + dCol)
    }

    private fun movePiece(row: Int, col: Int): Boolean {
        val moving = currentPiece ?: return false
        if (canMove(moving, row, col)) {
            moving.move(moving.pieceRow + row, moving.pieceCol + col)
            return true
        }
        return false
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
                if (this.totalLinesCleared >= settings.goalValue) {
                    AppLog.info { "Goal reached! Terminating Game." }
                    gameState = GameState.VICTORY
                    gameEventBus.post(GameEvent.GameOver(true, settings.goalType))
                }
            }

            else -> {}
        }
    }

}