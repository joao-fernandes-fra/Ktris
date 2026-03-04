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
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.util.concurrent.CopyOnWriteArraySet
import javax.swing.JPanel
import kotlin.math.min
import kotlin.math.sin


class SwingRenderer<T : Piece>(
    private val scoreRegistry: ScoreRegistry,
    private val tetrisEngine: TetrisEngine<*>,
) : JPanel(), GameRenderer<T> {

    companion object {
        private const val SCREEN_HEIGHT = 720
        private const val SCREEN_ASPECT_RATIO = 16f / 9f

        private const val FLASH_DURATION_MS = 200L
        private const val MESSAGE_DURATION_MS = 1500L

        private const val GHOST_PIECE_OPACITY = 30
        private const val FULL_OPACITY = 100

        private const val NEXT_PIECES_VERTICAL_SPACING = 4

        private const val HUD_WIDTH = 150
        private const val HUD_HEIGHT = 150
        private const val HUD_LINE_HEIGHT = 25
        private const val HUD_FONT_SIZE = 18
        private const val HUD_PADDING = 10
        private const val HUD_LINE_PADDING = 30


        private const val BOARD_BORDER_WIDTH = 2
        private const val BLOCK_PADDING = 2
        private const val GLOW_BLOCK_PADDING = 6

        private const val BACKGROUND_ALPHA = 128
        private const val GLOW_ALPHA_BASE = 100
        private const val GLOW_INNER_ALPHA = 200

        private const val HUD_FONT = "SansSerif"
        private const val SPIN_EFFECT_DURATION_MS = 300L
        private const val LERP_SPEED = 0.15f

    }

    private var lastSnapshot: GameSnapshot<T>? = null
    private var blockSize = HUD_LINE_PADDING

    private var flashAlpha = 0f
    private var lastClearTime = 0L

    private val messageQueue = CopyOnWriteArraySet<GameMessage>()
    private val messageYAnimation = mutableMapOf<GameMessage, Float>()
    private var gameFinished = false
    private var goalMet = false
    private var finishMessage: String? = null

    private var spinEffectStartTime = 0L
    private var lastSpinPieceState: PieceState<T>? = null
    private var lastSpinType: SpinType? = null

    init {
        val screenWidth = (SCREEN_HEIGHT * SCREEN_ASPECT_RATIO).toInt()
        preferredSize = Dimension(screenWidth, SCREEN_HEIGHT)
        background = Color.BLACK

        subscribeToGameEvents()
    }

    private fun subscribeToGameEvents() {
        EventHandler.subscribeToEvent<GameEvent.LineCleared> { event ->
            if (event.linesCleared > 0) {
                if (event.isPerfectClear) {
                    queueMessage("ALL CLEAR!", MessagePriority.HIGH)
                }
                triggerFlashEffect()
            }
        }

        EventHandler.subscribeToEvent<GameEvent.ScoreUpdated> { event ->
            val moveTypeName = event.moveTypeName
            if (!moveTypeName.isNullOrEmpty()) {
                queueMessage(moveTypeName, MessagePriority.MEDIUM)
            }
        }

        EventHandler.subscribeToEvent<GameEvent.ComboTriggered> { event ->
            queueMessage("COMBO x${event.comboCount}", MessagePriority.HIGH)
        }
        EventHandler.subscribeToEvent<GameEvent.BackToBackTrigger> { event ->
            queueMessage("B2B x${event.backToBackCount}", MessagePriority.MEDIUM)
        }

        EventHandler.subscribeToEvent<GameEvent.GarbageSent> {
            queueMessage("INCOMING ${it.lines} GARBAGE LINES", MessagePriority.LOW)
        }

        EventHandler.subscribeToEvent<GameEvent.GameOver> {
            gameFinished = true
            goalMet = it.goalMet
            finishMessage = if (it.goalMet) "VICTORY!" else "GAME OVER"
            repaint()
        }
        EventHandler.subscribeToEvent<GameEvent.SpinDetected> { event ->
            lastSpinPieceState = lastSnapshot?.currentPiece
            lastSpinType = event.spinType
            spinEffectStartTime = System.currentTimeMillis()
        }
    }

    override fun render(state: GameSnapshot<T>) {
        lastSnapshot = state
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val graphics = g as Graphics2D
        val snapshot = lastSnapshot ?: return

        calculateBlockSize(snapshot)
        drawFlashEffect(graphics)
        drawGameBoard(graphics, snapshot)
        if (gameFinished) {
            drawFinishScreen(graphics)
        }
    }

    private fun drawSpinEffect(graphics: Graphics2D, offsetX: Int, offsetY: Int) {
        val piece = lastSpinPieceState ?: return
        val elapsedTime = System.currentTimeMillis() - spinEffectStartTime
        if (elapsedTime >= SPIN_EFFECT_DURATION_MS) return

        val progress = elapsedTime.toFloat() / SPIN_EFFECT_DURATION_MS
        val alpha = (1.0f - progress) * 200
        val scale = 1.0f + (progress * 0.5f)

        val baseColor = if (lastSpinType == SpinType.FULL) Color.WHITE else Color.CYAN
        graphics.color = baseColor.withAlpha(alpha.toInt())

        // Draw an "aura" pulse around the piece
        graphics.stroke = BasicStroke(3f * (1.0f - progress))
        for (row in 0 until piece.shape.rows) {
            for (col in 0 until piece.shape.cols) {
                if (piece.shape[row, col] != 0) {
                    val x = offsetX + (piece.col + col) * blockSize
                    val y = offsetY + (piece.row + row) * blockSize
                    // Draw a slightly larger, outlined box
                    val sizeOffset = (blockSize * (scale - 1.0f)).toInt()
                    graphics.drawRect(
                        x - sizeOffset, y - sizeOffset,
                        blockSize + sizeOffset * 2, blockSize + sizeOffset * 2
                    )
                }
            }
        }
    }

    private fun drawFinishScreen(graphics: Graphics2D) {
        val text = finishMessage ?: return
        graphics.color = if (goalMet) Color.GREEN else Color.RED
        graphics.font = Font(HUD_FONT, Font.BOLD, 48)
        val metrics = graphics.fontMetrics
        val x = (width - metrics.stringWidth(text)) / 2
        val y = height / 2
        graphics.drawString(text, x, y)
    }

    private fun calculateBlockSize(snapshot: GameSnapshot<T>) {
        val padding = 12
        val maxWidth = width / (snapshot.board.cols + padding)
        val maxHeight = height / snapshot.board.rows
        blockSize = min(maxWidth, maxHeight)
    }

    private fun triggerFlashEffect() {
        flashAlpha = 1.0f
        lastClearTime = System.currentTimeMillis()
    }

    private fun queueMessage(text: String, priority: MessagePriority) {
        val now = System.currentTimeMillis()
        messageQueue.add(GameMessage(text, priority, now))

        messageQueue.sortedWith(
            compareByDescending<GameMessage> { it.priority.level }
                .thenBy { it.timestamp }
        )
    }

    private fun drawAllNotifications(graphics: Graphics2D, boardOffsetX: Int, uiOffsetY: Int) {
        val now = System.currentTimeMillis()
        messageQueue.removeIf { now - it.timestamp >= MESSAGE_DURATION_MS }

        var targetY = HUD_LINE_PADDING + HUD_FONT_SIZE + uiOffsetY

        for ((index, msg) in messageQueue.withIndex()) {
            val cascadeFactor = 1.0f - (index * 0.05f).coerceAtMost(0.4f)
            val dynamicLerp = LERP_SPEED * cascadeFactor
            val startY = messageYAnimation.getOrDefault(msg, targetY + 100f)
            val newY = startY + (targetY - startY) * dynamicLerp
            messageYAnimation[msg] = newY
            drawNotification(graphics, msg.text, boardOffsetX, newY.toInt())
            targetY += HUD_LINE_PADDING
        }

        messageYAnimation.keys.retainAll(messageQueue.toSet())
    }

    private fun drawNotification(
        graphics: Graphics2D,
        message: String,
        textX: Int,
        textY: Int
    ) {
        graphics.font = Font(HUD_FONT, Font.BOLD, HUD_FONT_SIZE)

        graphics.color = Color.WHITE
        graphics.drawString(message, textX, textY)
    }

    private fun drawFlashEffect(graphics: Graphics2D) {
        val elapsedTime = System.currentTimeMillis() - lastClearTime

        if (elapsedTime < FLASH_DURATION_MS) {
            flashAlpha = 1.0f - (elapsedTime.toFloat() / FLASH_DURATION_MS)

            val alphaValue = (flashAlpha * BACKGROUND_ALPHA).toInt()
            graphics.color = Color(255, 255, 255, alphaValue)
            graphics.fillRect(0, 0, width, height)
        }
    }

    private fun drawGameBoard(graphics: Graphics2D, snapshot: GameSnapshot<T>) {
        val boardPixelWidth = snapshot.board.cols * blockSize
        val boardPixelHeight = snapshot.board.rows * blockSize

        val boardOffsetX = (width - boardPixelWidth) / 2
        val boardOffsetY = (height - boardPixelHeight) / 2

        val bufferOffset = snapshot.board.bufferSize * blockSize

        val uiOffsetY = boardOffsetY + bufferOffset

        drawBoardBlocks(graphics, snapshot, boardOffsetX, boardOffsetY)
        drawGhostPiece(graphics, snapshot, boardOffsetX, boardOffsetY)
        drawSpinEffect(graphics, boardOffsetX, boardOffsetY)
        drawCurrentPiece(graphics, snapshot, boardOffsetX, boardOffsetY)

        drawBoardBorder(graphics, snapshot, boardOffsetX, uiOffsetY)

        drawHoldPiece(graphics, snapshot, boardOffsetX, uiOffsetY)
        drawNextPieces(graphics, snapshot, boardOffsetX, uiOffsetY)
        val hudX = boardOffsetX - (6 * blockSize)
        val hudY = uiOffsetY + (4 * blockSize)
        val currenHUDY = drawHUD(graphics, hudX, hudY)
        drawAllNotifications(graphics, hudX, currenHUDY)
    }

    private fun drawBoardBorder(
        graphics: Graphics2D,
        snapshot: GameSnapshot<T>,
        offsetX: Int,
        offsetY: Int
    ) {
        val leftX = offsetX - BOARD_BORDER_WIDTH
        val rightX = offsetX + (snapshot.board.cols * blockSize) + BOARD_BORDER_WIDTH
        val topY = offsetY - BOARD_BORDER_WIDTH
        val bottomY = offsetY + (snapshot.board.visibleRows * blockSize) + BOARD_BORDER_WIDTH

        graphics.color = Color.WHITE
        graphics.stroke = BasicStroke(BOARD_BORDER_WIDTH.toFloat())

        graphics.drawLine(leftX, topY, leftX, bottomY)
        graphics.drawLine(rightX, topY, rightX, bottomY)
        graphics.drawLine(leftX, bottomY, rightX, bottomY)
    }

    private fun drawBoardBlocks(
        graphics: Graphics2D,
        snapshot: GameSnapshot<T>,
        offsetX: Int,
        offsetY: Int
    ) {
        for (row in 0 until snapshot.board.rows) {
            for (col in 0 until snapshot.board.cols) {
                val blockId = snapshot.board[row, col]

                when {
                    blockId == -1 -> {
                        drawGlowingBlock(graphics, row, col, offsetX, offsetY)
                    }

                    blockId != 0 -> {
                        val color = getTetrominoColor(blockId)
                        drawBlock(graphics, row, col, color, offsetX, offsetY)
                    }
                }
            }
        }
    }

    private fun drawCurrentPiece(
        graphics: Graphics2D,
        snapshot: GameSnapshot<T>,
        offsetX: Int,
        offsetY: Int
    ) {
        snapshot.currentPiece?.let { piece ->
            drawPiece(graphics, piece, offsetX, offsetY, FULL_OPACITY)
        }
    }

    private fun drawGhostPiece(
        graphics: Graphics2D,
        snapshot: GameSnapshot<T>,
        offsetX: Int,
        offsetY: Int
    ) {
        snapshot.ghostPiece?.let { piece ->
            drawPiece(graphics, piece, offsetX, offsetY, GHOST_PIECE_OPACITY)
        }
    }

    private fun drawHoldPiece(graphics: Graphics2D, snapshot: GameSnapshot<T>, boardOffsetX: Int, boardOffsetY: Int) {
        snapshot.holdPiece?.let { piece ->
            val holdX = boardOffsetX - (5 * blockSize)
            drawPiece(graphics, piece.toPieceState(), holdX, boardOffsetY, FULL_OPACITY)
        }
    }

    private fun drawNextPieces(graphics: Graphics2D, snapshot: GameSnapshot<T>, boardOffsetX: Int, boardOffsetY: Int) {
        val nextX = boardOffsetX + (snapshot.board.cols * blockSize) + blockSize

        snapshot.nextPieces.forEachIndexed { index, piece ->
            piece?.let {
                val nextY = boardOffsetY + (index * NEXT_PIECES_VERTICAL_SPACING * blockSize)
                drawPiece(graphics, it.toPieceState(), nextX, nextY, FULL_OPACITY)
            }
        }
    }

    private fun drawPiece(
        graphics: Graphics2D,
        piece: PieceState<T>,
        offsetX: Int,
        offsetY: Int,
        opacity: Int
    ) {
        val baseColor = getTetrominoColor(piece.type.id)
        val alphaValue = (opacity * 255 / FULL_OPACITY).coerceIn(0, 255)
        val color = baseColor.withAlpha(alphaValue)

        for (row in 0 until piece.shape.rows) {
            for (col in 0 until piece.shape.cols) {
                if (piece.shape[row, col] != 0) {
                    drawBlock(
                        graphics,
                        piece.row + row,
                        piece.col + col,
                        color,
                        offsetX,
                        offsetY
                    )
                }
            }
        }
    }

    private fun drawBlock(
        graphics: Graphics2D,
        row: Int,
        col: Int,
        color: Color,
        offsetX: Int,
        offsetY: Int
    ) {
        val x = offsetX + col * blockSize
        val y = offsetY + row * blockSize
        val blockWidth = blockSize - BLOCK_PADDING
        val blockHeight = blockSize - BLOCK_PADDING

        graphics.color = color
        graphics.fillRect(x, y, blockWidth, blockHeight)
    }

    private fun drawGlowingBlock(
        graphics: Graphics2D,
        row: Int,
        col: Int,
        offsetX: Int,
        offsetY: Int
    ) {
        val x = offsetX + col * blockSize
        val y = offsetY + row * blockSize

        val pulseFactor = (sin(System.currentTimeMillis() / 150.0) * 0.25 + 0.55).toFloat()
        val glowAlpha = (pulseFactor * GLOW_ALPHA_BASE).toInt()

        graphics.color = Color(255, 255, 255, glowAlpha)
        graphics.fillRect(
            x - BLOCK_PADDING, y - BLOCK_PADDING,
            blockSize + BLOCK_PADDING * 2, blockSize + BLOCK_PADDING * 2
        )

        graphics.color = Color.WHITE
        graphics.fillRect(x, y, blockSize - BLOCK_PADDING, blockSize - BLOCK_PADDING)

        graphics.stroke = BasicStroke(1f)
        graphics.color = Color(255, 255, 255, GLOW_INNER_ALPHA)
        graphics.drawRect(
            x + BLOCK_PADDING / 2, y + BLOCK_PADDING / 2,
            blockSize - GLOW_BLOCK_PADDING, blockSize - GLOW_BLOCK_PADDING
        )
    }

    private fun drawHUD(graphics: Graphics2D, hudX: Int, hudY: Int): Int {

        graphics.color = Color(0, 0, 0, 150)
        graphics.fillRect(hudX, hudY, HUD_WIDTH, HUD_HEIGHT)

        graphics.color = Color.WHITE
        graphics.font = Font(HUD_FONT, Font.BOLD, HUD_FONT_SIZE)

        var currentY = hudY + HUD_LINE_PADDING

        drawHUDLine(graphics, "SCORE", hudX + HUD_PADDING, currentY)
        currentY += HUD_LINE_HEIGHT
        drawHUDLine(graphics, scoreRegistry.totalPoints.toInt().toString(), hudX + HUD_PADDING, currentY)

        currentY += HUD_LINE_HEIGHT
        drawHUDLine(graphics, "LINES", hudX + HUD_PADDING, currentY)
        currentY += HUD_LINE_HEIGHT
        drawHUDLine(graphics, scoreRegistry.totalLinesCleared.toString(), hudX + HUD_PADDING, currentY)

        currentY += HUD_LINE_HEIGHT
        drawHUDLine(graphics, "TIME", hudX + HUD_PADDING, currentY)
        currentY += HUD_LINE_HEIGHT
        drawHUDLine(graphics, formatTime(tetrisEngine.sessionTimeSeconds), hudX + HUD_PADDING, currentY)
        return currentY
    }

    private fun drawHUDLine(graphics: Graphics2D, text: String, x: Int, y: Int) {
        graphics.drawString(text, x, y)
    }

    private fun getTetrominoColor(id: Int): Color = when (id) {
        Tetromino.I.id -> Color.CYAN
        Tetromino.O.id -> Color.YELLOW
        Tetromino.T.id -> Color.MAGENTA
        Tetromino.S.id -> Color.GREEN
        Tetromino.Z.id -> Color.RED
        Tetromino.J.id -> Color.BLUE
        Tetromino.L.id -> Color.ORANGE
        else -> Color.LIGHT_GRAY
    }

    private fun Color.withAlpha(alpha: Int): Color {
        return Color(red, green, blue, alpha.coerceIn(0, 255))
    }

    private fun formatTime(seconds: Float): String {
        val totalSeconds = seconds.toLong()
        val minutes = (totalSeconds / 60) % 60
        val remainingSeconds = totalSeconds % 60
        return "$minutes:$remainingSeconds"
    }
}