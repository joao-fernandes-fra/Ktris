package demo

import controller.GameRenderer
import controller.TetrisEngine
import controller.defaults.ScoreRegistry
import model.GameSnapshot
import model.Piece
import model.PieceState
import model.SpinType
import model.defaults.Tetromino
import model.events.EventHandler
import model.events.GameEvent
import model.toPieceState
import org.w3c.dom.CanvasRenderingContext2D
import kotlin.js.Date
import kotlin.math.min
import kotlin.math.sin

class WebRenderer<T : Piece>(
    private val scoreRegistry: ScoreRegistry,
    private val tetrisEngine: TetrisEngine<*>,
    private val ctx: CanvasRenderingContext2D
) : GameRenderer<T> {

    companion object {
        private const val FLASH_DURATION_MS = 200.0
        private const val MESSAGE_DURATION_MS = 1500.0
        private const val GHOST_OPACITY = 0.3
        private const val BLOCK_PADDING = 2
        private const val BOARD_BORDER_WIDTH = 2
        private const val HUD_FONT = "16px sans-serif"
        private const val HUD_LINE_HEIGHT = 20
    }

    private var lastSnapshot: GameSnapshot<T>? = null
    private var blockSize = 30
    private var flashAlpha = 0.0
    private var lastClearTime = 0.0
    private var gameFinished = false
    private var goalMet = false
    private var finishMessage: String? = null

    private val messages = mutableListOf<GameMessage>()
    private var spinEffectStartTime = 0.0
    private var lastSpinPieceState: PieceState<T>? = null
    private var lastSpinType: SpinType? = null

    init {
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        EventHandler.subscribeToEvent<GameEvent.LineCleared> { event ->
            if (event.linesCleared > 0) {
                if (event.isPerfectClear) queueMessage("ALL CLEAR!")
                triggerFlash()
            }
        }
        EventHandler.subscribeToEvent<GameEvent.ScoreUpdated> { event ->
            event.moveTypeName?.let { queueMessage(it) }
        }
        EventHandler.subscribeToEvent<GameEvent.ComboTriggered> { event ->
            queueMessage("COMBO x${event.comboCount}")
        }
        EventHandler.subscribeToEvent<GameEvent.BackToBackTrigger> { event ->
            queueMessage("B2B x${event.backToBackCount}")
        }
        EventHandler.subscribeToEvent<GameEvent.GarbageSent> {
            queueMessage("INCOMING ${it.lines} GARBAGE")
        }
        EventHandler.subscribeToEvent<GameEvent.GameOver> {
            gameFinished = true
            goalMet = it.goalMet
            finishMessage = if (it.goalMet) "VICTORY!" else "GAME OVER"
        }
        EventHandler.subscribeToEvent<GameEvent.SpinDetected> { event ->
            lastSpinPieceState = lastSnapshot?.currentPiece
            lastSpinType = event.spinType
            spinEffectStartTime = Date.now()
        }
    }

    override fun render(state: GameSnapshot<T>) {
        lastSnapshot = state
        draw()
    }

    private fun draw() {
        val snapshot = lastSnapshot ?: return
        val canvasWidth = ctx.canvas.width.toDouble()
        val canvasHeight = ctx.canvas.height.toDouble()

        ctx.fillStyle = "black"
        ctx.fillRect(0.0, 0.0, canvasWidth, canvasHeight)

        calculateBlockSize(snapshot)
        drawBoard(snapshot)
        if (gameFinished) drawFinishScreen()
        drawFlash()
        drawMessages()
    }

    private fun calculateBlockSize(snapshot: GameSnapshot<T>) {
        val maxWidth = ctx.canvas.width / (snapshot.board.cols + 8)
        val maxHeight = ctx.canvas.height / snapshot.board.rows
        blockSize = min(maxWidth, maxHeight)
    }

    private fun drawBoard(snapshot: GameSnapshot<T>) {
        val cols = snapshot.board.cols
        val rows = snapshot.board.rows
        val boardWidth = cols * blockSize
        val boardHeight = rows * blockSize
        val offsetX = (ctx.canvas.width - boardWidth) / 2
        val offsetY = (ctx.canvas.height - boardHeight) / 2

        // Draw blocks
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val blockId = snapshot.board[row, col]
                when {
                    blockId == -1 -> drawGlowingBlock(offsetX + col * blockSize, offsetY + row * blockSize)
                    blockId != 0 -> {
                        val color = getTetrominoColor(blockId)
                        drawBlock(offsetX + col * blockSize, offsetY + row * blockSize, color)
                    }
                }
            }
        }

        // Draw ghost piece
        snapshot.ghostPiece?.let { drawPiece(it, offsetX, offsetY, GHOST_OPACITY) }

        // Draw current piece
        snapshot.currentPiece?.let { drawPiece(it, offsetX, offsetY, 1.0) }

        // Draw spin effect
        drawSpinEffect(offsetX, offsetY)

        // Draw hold piece
        snapshot.holdPiece?.let {
            val holdX = offsetX - 5 * blockSize
            drawPiece(it.toPieceState(), holdX, offsetY, 1.0)
        }

        // Draw next pieces
        snapshot.nextPieces.forEachIndexed { index, piece ->
            piece?.let {
                val nextX = offsetX + (cols * blockSize) + blockSize
                val nextY = offsetY + index * 4 * blockSize
                drawPiece(it.toPieceState(), nextX, nextY, 1.0)
            }
        }

        // Draw board border
        ctx.strokeStyle = "white"
        ctx.lineWidth = BOARD_BORDER_WIDTH.toDouble()
        ctx.strokeRect(offsetX.toDouble(), offsetY.toDouble(), boardWidth.toDouble(), (snapshot.board.visibleRows * blockSize).toDouble())

        // Draw HUD
        drawHUD(offsetX - 6 * blockSize, offsetY + 4 * blockSize)
    }

    private fun drawPiece(piece: PieceState<T>, offsetX: Int, offsetY: Int, opacity: Double) {
        val baseColor = getTetrominoColor(piece.type.id)
        val color = adjustOpacity(baseColor, opacity)
        ctx.fillStyle = color

        for (row in 0 until piece.shape.rows) {
            for (col in 0 until piece.shape.cols) {
                if (piece.shape[row, col] != 0) {
                    val x = offsetX + (piece.col + col) * blockSize
                    val y = offsetY + (piece.row + row) * blockSize
                    ctx.fillRect(x.toDouble(), y.toDouble(), (blockSize - BLOCK_PADDING).toDouble(), (blockSize - BLOCK_PADDING).toDouble())
                }
            }
        }
    }

    private fun drawBlock(x: Int, y: Int, color: String) {
        ctx.fillStyle = color
        ctx.fillRect(x.toDouble(), y.toDouble(), (blockSize - BLOCK_PADDING).toDouble(), (blockSize - BLOCK_PADDING).toDouble())
    }

    private fun drawGlowingBlock(x: Int, y: Int) {
        val pulse = (sin(Date.now() / 150.0) * 0.25 + 0.55).toFloat()
        val glowAlpha = (pulse * 100).toInt()
        val glowColor = "rgba(255,255,255,${glowAlpha / 255.0})"
        ctx.fillStyle = glowColor
        ctx.fillRect((x - BLOCK_PADDING).toDouble(), (y - BLOCK_PADDING).toDouble(), (blockSize + 2 * BLOCK_PADDING).toDouble(), (blockSize + 2 * BLOCK_PADDING).toDouble())

        ctx.fillStyle = "white"
        ctx.fillRect(x.toDouble(), y.toDouble(), (blockSize - BLOCK_PADDING).toDouble(), (blockSize - BLOCK_PADDING).toDouble())

        ctx.strokeStyle = "rgba(255,255,255,0.8)"
        ctx.lineWidth = 1.0
        ctx.strokeRect((x + BLOCK_PADDING / 2).toDouble(), (y + BLOCK_PADDING / 2).toDouble(), (blockSize - 6).toDouble(), (blockSize - 6).toDouble())
    }

    private fun drawSpinEffect(offsetX: Int, offsetY: Int) {
        val piece = lastSpinPieceState ?: return
        val elapsed = Date.now() - spinEffectStartTime
        if (elapsed >= 300) return

        val progress = (elapsed / 300.0).toFloat()
        val alpha = (1.0f - progress) * 0.8f
        val scale = 1.0f + (progress * 0.5f)

        ctx.strokeStyle = if (lastSpinType == SpinType.FULL) "rgba(255,255,255,$alpha)" else "rgba(0,255,255,$alpha)"
        ctx.lineWidth = 3.0 * (1.0f - progress)

        for (row in 0 until piece.shape.rows) {
            for (col in 0 until piece.shape.cols) {
                if (piece.shape[row, col] != 0) {
                    val x = offsetX + (piece.col + col) * blockSize
                    val y = offsetY + (piece.row + row) * blockSize
                    val sizeOffset = (blockSize * (scale - 1.0f)).toInt()
                    ctx.strokeRect(
                        (x - sizeOffset).toDouble(), (y - sizeOffset).toDouble(),
                        (blockSize + 2 * sizeOffset).toDouble(), (blockSize + 2 * sizeOffset).toDouble()
                    )
                }
            }
        }
    }

    private fun drawHUD(hudX: Int, hudY: Int) {
        ctx.font = HUD_FONT
        ctx.fillStyle = "rgba(0,0,0,0.7)"
        ctx.fillRect(hudX.toDouble(), hudY.toDouble(), 150.0, 150.0)

        ctx.fillStyle = "white"
        var y = hudY + 20
        ctx.fillText("SCORE", hudX + 10.0, y.toDouble())
        y += HUD_LINE_HEIGHT
        ctx.fillText(scoreRegistry.totalPoints.toInt().toString(), hudX + 10.0, y.toDouble())
        y += HUD_LINE_HEIGHT
        ctx.fillText("LINES", hudX + 10.0, y.toDouble())
        y += HUD_LINE_HEIGHT
        ctx.fillText(scoreRegistry.totalLinesCleared.toString(), hudX + 10.0, y.toDouble())
        y += HUD_LINE_HEIGHT
        ctx.fillText("TIME", hudX + 10.0, y.toDouble())
        y += HUD_LINE_HEIGHT
        ctx.fillText(formatTime(tetrisEngine.sessionTimeSeconds), hudX + 10.0, y.toDouble())
    }

    private fun drawFinishScreen() {
        ctx.font = "bold 48px sans-serif"
        ctx.fillStyle = if (goalMet) "green" else "red"
        val text = finishMessage ?: return
        val metrics = ctx.measureText(text)
        val x = (ctx.canvas.width - metrics.width) / 2
        val y = ctx.canvas.height / 2
        ctx.fillText(text, x, y.toDouble())
    }

    private fun drawFlash() {
        val elapsed = Date.now() - lastClearTime
        if (elapsed < FLASH_DURATION_MS) {
            flashAlpha = 1.0 - (elapsed / FLASH_DURATION_MS)
            ctx.fillStyle = "rgba(255,255,255,${flashAlpha * 0.5})"
            ctx.fillRect(0.0, 0.0, ctx.canvas.width.toDouble(), ctx.canvas.height.toDouble())
        }
    }

    private fun drawMessages() {
        val now = Date.now()
        messages.removeAll { now - it.timestamp >= MESSAGE_DURATION_MS }
        var y = 50
        for (msg in messages) {
            ctx.font = "bold 20px sans-serif"
            ctx.fillStyle = "white"
            ctx.fillText(msg.text, 20.0, y.toDouble())
            y += 25
        }
    }

    private fun queueMessage(text: String) {
        messages.add(GameMessage(text, Date.now()))
    }

    private fun triggerFlash() {
        flashAlpha = 1.0
        lastClearTime = Date.now()
    }

    private fun getTetrominoColor(id: Int): String = when (id) {
        Tetromino.I.id -> "cyan"
        Tetromino.O.id -> "yellow"
        Tetromino.T.id -> "magenta"
        Tetromino.S.id -> "green"
        Tetromino.Z.id -> "red"
        Tetromino.J.id -> "blue"
        Tetromino.L.id -> "orange"
        else -> "lightgray"
    }

    private fun adjustOpacity(color: String, opacity: Double): String {
        return when (color) {
            "cyan" -> "rgba(0,255,255,$opacity)"
            "yellow" -> "rgba(255,255,0,$opacity)"
            "magenta" -> "rgba(255,0,255,$opacity)"
            "green" -> "rgba(0,255,0,$opacity)"
            "red" -> "rgba(255,0,0,$opacity)"
            "blue" -> "rgba(0,0,255,$opacity)"
            "orange" -> "rgba(255,165,0,$opacity)"
            else -> "rgba(211,211,211,$opacity)"
        }
    }

    private fun formatTime(seconds: Float): String {
        val total = seconds.toInt()
        val mins = (total / 60) % 60
        val secs = total % 60
        return "$mins:$secs"
    }

    data class GameMessage(val text: String, val timestamp: Double)
}