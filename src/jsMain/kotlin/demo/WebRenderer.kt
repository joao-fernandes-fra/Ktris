package demo

import controller.GameRenderer
import controller.defaults.ScoreRegistry
import model.GameSnapshot
import model.GameTimers
import model.Piece
import model.PieceState
import model.SpinType
import model.defaults.Tetromino
import model.events.EventHandler
import model.events.GameEvent
import model.toPieceState
import org.w3c.dom.CanvasRenderingContext2D
import platform.currentTimeMillis
import kotlin.js.Date
import kotlin.math.min
import kotlin.math.sin

class WebRenderer<T : Piece>(
    private val scoreRegistry: ScoreRegistry,
    private val gameTimers: GameTimers,
    private val ctx: CanvasRenderingContext2D
) : GameRenderer<T> {

    companion object {
        private const val SCREEN_HEIGHT = 720
        private const val SCREEN_ASPECT_RATIO = 16 / 9

        private const val FLASH_DURATION_MS = 200L
        private const val MESSAGE_DURATION_MS = 1500L

        private const val GHOST_PIECE_OPACITY = 0.3
        private const val FULL_OPACITY = 1.0

        private const val NEXT_PIECES_VERTICAL_SPACING = 4.0

        private const val HUD_WIDTH = 150.0
        private const val HUD_HEIGHT = 150.0
        private const val HUD_LINE_HEIGHT = 25.0
        private const val HUD_FONT_SIZE = 18.0
        private const val HUD_PADDING = 10.0
        private const val HUD_LINE_PADDING = 30.0


        private const val BOARD_BORDER_WIDTH = 2.0
        private const val BLOCK_PADDING = 2.0
        private const val GLOW_BLOCK_PADDING = 6

        private const val BACKGROUND_ALPHA = 128.0
        private const val GLOW_ALPHA_BASE = 100.0
        private const val GLOW_INNER_ALPHA = 200.0

        private const val HUD_FONT = "SansSerif"
        private const val SPIN_EFFECT_DURATION_MS = 300L
        private const val LERP_SPEED = 0.15f

    }

    init {
        ctx.canvas.width = SCREEN_HEIGHT * SCREEN_ASPECT_RATIO
        ctx.canvas.height = SCREEN_HEIGHT
        subscribeToEvents()
    }

    private var lastSnapshot: GameSnapshot<T>? = null
    private var blockSize = 0

    private var flashAlpha = 0.0
    private var lastClearTime = 0.0

    private var messageQueue = mutableSetOf<GameMessage>()
    private val messageYAnimation = mutableMapOf<GameMessage, Double>()
    private var gameFinished = false
    private var goalMet = false
    private var finishMessage: String? = null

    private var spinEffectStartTime = 0L
    private var lastSpinPieceState: PieceState<T>? = null
    private var lastSpinType: SpinType? = null

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
            spinEffectStartTime = Date.now().toLong()
        }
    }

    override fun render(state: GameSnapshot<T>) {
        lastSnapshot = state
        draw()
    }

    private fun draw() {
        val snapshot = lastSnapshot ?: return
        val canvasWidth = ctx.canvas.width
        val canvasHeight = ctx.canvas.height

        ctx.fillStyle = "black"
        ctx.fillRect(0.0, 0.0, canvasWidth.toDouble(), canvasHeight.toDouble())

        calculateBlockSize(snapshot)
        if (gameFinished) {
            drawFinishScreen()
        }
        drawFlash()
        drawGameBoard(snapshot)
    }

    private fun calculateBlockSize(snapshot: GameSnapshot<T>) {
        val padding = 12
        val maxWidth = ctx.canvas.width / (snapshot.board.cols + padding)
        val maxHeight = ctx.canvas.height / snapshot.board.rows
        blockSize = min(maxWidth, maxHeight)
    }

    private fun drawGameBoard(snapshot: GameSnapshot<T>) {
        val boardPixelWidth = snapshot.board.cols * blockSize
        val boardPixelHeight = snapshot.board.rows * blockSize

        val boardOffsetX = (ctx.canvas.width - boardPixelWidth) / 2
        val boardOffsetY = (ctx.canvas.height - boardPixelHeight) / 2

        val bufferOffset = snapshot.board.bufferSize * blockSize

        val uiOffsetY = boardOffsetY + bufferOffset

        drawBoardBlocks(snapshot, boardOffsetX, boardOffsetY)
        drawGhostPiece(snapshot, boardOffsetX, boardOffsetY)
        drawSpinEffect(boardOffsetX, boardOffsetY)
        drawCurrentPiece(snapshot, boardOffsetX, boardOffsetY)

        drawBoardBorder(snapshot, boardOffsetX, uiOffsetY)

        drawHoldPiece(snapshot, boardOffsetX, uiOffsetY)
        drawNextPieces(snapshot, boardOffsetX, uiOffsetY)
        val hudX = boardOffsetX - (6 * blockSize)
        val hudY = uiOffsetY + (4 * blockSize)
        val currenHUDY = drawHUD(hudX, hudY)
        drawAllNotifications(hudX, currenHUDY.toInt())
    }

    private fun drawBoardBlocks(
        snapshot: GameSnapshot<T>,
        offsetX: Int,
        offsetY: Int
    ) {
        for (row in 0 until snapshot.board.rows) {
            for (col in 0 until snapshot.board.cols) {
                val blockId = snapshot.board[row, col]

                when {
                    blockId == -1 -> {
                        drawGlowingBlock(row, col, offsetX, offsetY)
                    }

                    blockId != 0 -> {
                        val color = adjustOpacity(getTetrominoColor(blockId), FULL_OPACITY)
                        drawBlock(row, col, offsetX, offsetY, color)
                    }
                }
            }
        }
    }

    private fun drawGhostPiece(
        snapshot: GameSnapshot<T>,
        offsetX: Int,
        offsetY: Int
    ) {
        snapshot.ghostPiece?.let { piece ->
            drawPiece(piece, offsetX, offsetY, GHOST_PIECE_OPACITY)
        }
    }

    private fun drawHoldPiece(snapshot: GameSnapshot<T>, boardOffsetX: Int, boardOffsetY: Int) {
        snapshot.holdPiece?.let { piece ->
            val holdX = boardOffsetX - (5 * blockSize)
            drawPiece(piece.toPieceState(), holdX, boardOffsetY, FULL_OPACITY)
        }
    }

    private fun drawCurrentPiece(snapshot: GameSnapshot<T>, boardOffsetX: Int, boardOffsetY: Int) {
        snapshot.currentPiece?.let { piece ->
            drawPiece(piece, boardOffsetX, boardOffsetY, FULL_OPACITY)
        }
    }

    private fun drawNextPieces(snapshot: GameSnapshot<T>, boardOffsetX: Int, boardOffsetY: Int) {
        val nextX = boardOffsetX + (snapshot.board.cols * blockSize) + blockSize

        snapshot.nextPieces.forEachIndexed { index, piece ->
            piece?.let {
                val nextY = boardOffsetY + (index * NEXT_PIECES_VERTICAL_SPACING * blockSize)
                drawPiece(it.toPieceState(), nextX, nextY.toInt(), FULL_OPACITY)
            }
        }
    }

    private fun drawBoardBorder(snapshot: GameSnapshot<T>, boardOffsetX: Int, uiOffsetY: Int) {
        ctx.strokeStyle = "white"
        ctx.lineWidth = BOARD_BORDER_WIDTH

        val totalWidth = (snapshot.board.cols * blockSize) + (2 * BOARD_BORDER_WIDTH)
        val totalHeight = (snapshot.board.visibleRows * blockSize) + (2 * BOARD_BORDER_WIDTH)

        val startX = boardOffsetX - BOARD_BORDER_WIDTH
        val startY = uiOffsetY - BOARD_BORDER_WIDTH
        ctx.beginPath()
        ctx.strokeStyle = "white"
        ctx.lineWidth = BOARD_BORDER_WIDTH
        ctx.moveTo(startX, startY)
        ctx.lineTo(startX, startY + totalHeight)
        ctx.lineTo(startX + totalWidth, startY + totalHeight)
        ctx.lineTo(startX + totalWidth, startY)
        ctx.stroke()
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
                    ctx.fillRect(
                        x.toDouble(),
                        y.toDouble(),
                        (blockSize - BLOCK_PADDING),
                        (blockSize - BLOCK_PADDING)
                    )
                }
            }
        }
    }

    private fun drawBlock(row: Int, col: Int, offsetX: Int, offsetY: Int, color: String) {
        val x = offsetX + col * blockSize
        val y = offsetY + row * blockSize
        val blockWidth = blockSize - BLOCK_PADDING
        val blockHeight = blockSize - BLOCK_PADDING
        ctx.fillStyle = color
        ctx.fillRect(
            x.toDouble(),
            y.toDouble(),
            blockWidth,
            blockHeight
        )
    }

    private fun drawGlowingBlock(row: Int, col: Int, offsetX: Int, offsetY: Int) {
        val x = offsetX + col * blockSize
        val y = offsetY + row * blockSize
        val blockWidth = blockSize - BLOCK_PADDING
        val blockHeight = blockSize - BLOCK_PADDING
        val pulse = (sin(Date.now() / 150.0) * 0.25 + 0.55).toFloat()
        val glowAlpha = (pulse * 100).toInt()
        val glowColor = "rgba(255,255,255,${glowAlpha / 255.0})"
        ctx.fillStyle = glowColor
        ctx.fillRect(
            (x - BLOCK_PADDING),
            (y - BLOCK_PADDING),
            (blockSize + 2 * BLOCK_PADDING),
            (blockSize + 2 * BLOCK_PADDING)
        )

        ctx.fillStyle = "white"
        ctx.fillRect(
            x.toDouble(),
            y.toDouble(),
            (blockSize - BLOCK_PADDING),
            (blockSize - BLOCK_PADDING)
        )

        ctx.strokeStyle = "rgba(255,255,255,0.8)"
        ctx.lineWidth = 1.0
        ctx.strokeRect(
            (x + BLOCK_PADDING / 2),
            (y + BLOCK_PADDING / 2),
            (blockSize - 6).toDouble(),
            (blockSize - 6).toDouble()
        )
    }

    private fun drawSpinEffect(offsetX: Int, offsetY: Int) {
        val piece = lastSpinPieceState ?: return
        val elapsed = Date.now() - spinEffectStartTime
        if (elapsed >= SPIN_EFFECT_DURATION_MS) return

        val progress = (elapsed / SPIN_EFFECT_DURATION_MS).toFloat()
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

    private fun drawHUD(hudX: Int, boardOffsetY: Int): Double {
        ctx.font = HUD_FONT
        ctx.fillStyle = "rgba(0,0,0,0.7)"
        ctx.fillRect(hudX.toDouble(), boardOffsetY.toDouble(), 150.0, 150.0)

        ctx.fillStyle = "white"
        var hudY = boardOffsetY + HUD_LINE_HEIGHT
        ctx.fillText("SCORE", hudX + HUD_PADDING, hudY)
        hudY += HUD_LINE_HEIGHT
        ctx.fillText(scoreRegistry.totalPoints.toInt().toString(), hudX + 10.0, hudY)
        hudY += HUD_LINE_HEIGHT
        ctx.fillText("LINES", hudX + 10.0, hudY)
        hudY += HUD_LINE_HEIGHT
        ctx.fillText(scoreRegistry.totalLinesCleared.toString(), hudX + 10.0, hudY)
        hudY += HUD_LINE_HEIGHT
        ctx.fillText("TIME", hudX + 10.0, hudY)
        hudY += HUD_LINE_HEIGHT
        ctx.fillText(formatTime(gameTimers.sessionTimer), hudX + 10.0, hudY)

        return hudY
    }

    private fun drawFinishScreen() {
        ctx.font = "bold 48px sans-serif"
        ctx.fillStyle = if (goalMet) "green" else "red"
        val text = finishMessage ?: return
        val metrics = ctx.measureText(text)
        val messageX = (ctx.canvas.width - metrics.width) / 2
        val messageY = ctx.canvas.height / 2
        ctx.fillText(text, messageX, messageY.toDouble())
    }

    private fun drawFlash() {
        val elapsed = Date.now() - lastClearTime
        if (elapsed < FLASH_DURATION_MS) {
            flashAlpha = 1.0 - (elapsed / FLASH_DURATION_MS)
            ctx.fillStyle = "rgba(255,255,255,${flashAlpha * 0.5})"
            ctx.fillRect(0.0, 0.0, ctx.canvas.width.toDouble(), ctx.canvas.height.toDouble())
        }
    }

    private fun drawAllNotifications(boardOffsetX: Int, uiOffsetY: Int) {
        val now = currentTimeMillis()

        messageQueue.removeAll { now - it.timestamp >= MESSAGE_DURATION_MS }

        var targetY = HUD_LINE_PADDING + HUD_FONT_SIZE + uiOffsetY

        for ((index, msg) in messageQueue.toList().withIndex()) {
            val cascadeFactor = 1.0f - (index * 0.05f).coerceAtMost(0.4f)
            val dynamicLerp = LERP_SPEED * cascadeFactor

            val startY = messageYAnimation[msg] ?: (targetY + 100f)
            val newY = startY + (targetY - startY) * dynamicLerp

            messageYAnimation[msg] = newY

            drawNotification(msg.text, boardOffsetX, newY.toInt())
            targetY += HUD_LINE_PADDING
        }

        messageYAnimation.keys.retainAll(messageQueue.toSet())
    }

    private fun drawNotification(text: String, boardOffsetX: Int, boardOffsetY: Int) {
        ctx.font = "bold 20px sans-serif"
        ctx.fillStyle = "white"
        ctx.fillText(text, boardOffsetX.toDouble(), boardOffsetY.toDouble())
    }

    private fun queueMessage(text: String) {
        messageQueue.add(GameMessage(text, Date.now()))
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
        val total = (seconds / 1000).toInt()
        val mins = (total / 60) % 60
        val secs = total % 60
        return "$mins:$secs"
    }

    data class GameMessage(val text: String, val timestamp: Double)
}