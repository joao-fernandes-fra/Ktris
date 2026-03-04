package demo

import controller.GameRenderer
import controller.TetrisEngine
import controller.defaults.ScoreRegistry
import model.GameSnapshot
import model.Piece
import model.PieceState
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

        private const val BOARD_OFFSET_COLS = 5
        private const val HOLD_PIECE_OFFSET = 25
        private const val NEXT_PIECES_OFFSET_COLS = 6
        private const val NEXT_PIECES_VERTICAL_SPACING = 4

        private const val HUD_WIDTH = 150
        private const val HUD_HEIGHT = 150
        private const val HUD_X_OFFSET = 10
        private const val HUD_LINE_HEIGHT = 25
        private const val HUD_FONT_SIZE = 18

        private const val BOARD_BORDER_WIDTH = 2
        private const val BLOCK_PADDING = 2
        private const val GLOW_BLOCK_PADDING = 6

        private const val BACKGROUND_ALPHA = 128
        private const val GLOW_ALPHA_BASE = 100
        private const val GLOW_INNER_ALPHA = 200

        private const val HUD_FONT = "SansSerif"
    }

    private var lastSnapshot: GameSnapshot<T>? = null
    private var blockSize = 30

    private var flashAlpha = 0f
    private var lastClearTime = 0L

    private var activeMessage: String? = null
    private var messageAlpha = 0f
    private var messageStartTime = 0L
    private var gameFinished = false
    private var goalMet = false
    private var finishMessage: String? = null

    init {
        val screenWidth = (SCREEN_HEIGHT * SCREEN_ASPECT_RATIO).toInt()
        preferredSize = Dimension(screenWidth, SCREEN_HEIGHT)
        background = Color.BLACK

        subscribeToGameEvents()
    }

    private fun subscribeToGameEvents() {
        EventHandler.subscribeToEvent<GameEvent.LineCleared> { event ->
            if (event.linesCleared > 0) {
                triggerFlashEffect()
            }
        }

        EventHandler.subscribeToEvent<GameEvent.ScoreUpdated> { event ->
            val moveTypeName = event.moveTypeName
            if (!moveTypeName.isNullOrEmpty()) {
                if (event.backToBackCount > 0) {
                    showTemporaryMessage("BACK TO BACK $moveTypeName")
                } else showTemporaryMessage(moveTypeName)
            }
        }

        EventHandler.subscribeToEvent<GameEvent.ComboTriggered> { event ->
            showTemporaryMessage("COMBO x${event.comboCount}")
        }

        EventHandler.subscribeToEvent<GameEvent.GarbageSent> {
            showTemporaryMessage("INCOMING ${it.lines} GARBAGE LINES")
        }

        EventHandler.subscribeToEvent<GameEvent.GameOver> {
            gameFinished = true
            goalMet = it.goalMet
            finishMessage = if (it.goalMet) "VICTORY!" else "GAME OVER"
            repaint()
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
        drawMessages(graphics, snapshot)
        if (gameFinished) {
            drawFinishScreen(graphics)
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

    private fun showTemporaryMessage(message: String) {
        activeMessage = message
        messageAlpha = 1.0f
        messageStartTime = System.currentTimeMillis()
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
        val boardOffsetX = BOARD_OFFSET_COLS * blockSize
        val borderYOffset = snapshot.board.bufferSize * blockSize
        drawHUD(graphics)
        drawBoardBorder(graphics, snapshot, boardOffsetX, borderYOffset)
        drawBoardBlocks(graphics, snapshot, boardOffsetX, 0)
        drawGhostPiece(graphics, snapshot, boardOffsetX, 0)
        drawCurrentPiece(graphics, snapshot, boardOffsetX, 0)
        drawHoldPiece(graphics, snapshot)
        drawNextPieces(graphics, snapshot)
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
                        val color = getTetrominoColor(blockId ?: 0)
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

    private fun drawHoldPiece(graphics: Graphics2D, snapshot: GameSnapshot<T>) {
        snapshot.holdPiece?.let { piece ->
            drawPiece(graphics, piece.toPieceState(), HOLD_PIECE_OFFSET, HOLD_PIECE_OFFSET, FULL_OPACITY)
        }
    }

    private fun drawNextPieces(graphics: Graphics2D, snapshot: GameSnapshot<T>) {
        snapshot.nextPieces.forEachIndexed { index, piece ->
            piece?.let {
                val offsetX = (snapshot.board.cols + NEXT_PIECES_OFFSET_COLS) * blockSize
                val offsetY = index * NEXT_PIECES_VERTICAL_SPACING * blockSize
                drawPiece(graphics, it.toPieceState(), offsetX, offsetY, FULL_OPACITY)
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

    private fun drawHUD(graphics: Graphics2D) {
        val hudX = HUD_X_OFFSET
        val hudY = BOARD_OFFSET_COLS * blockSize

        graphics.color = Color(0, 0, 0, 150)
        graphics.fillRect(hudX, hudY, HUD_WIDTH, HUD_HEIGHT)

        graphics.color = Color.WHITE
        graphics.font = Font(HUD_FONT, Font.BOLD, HUD_FONT_SIZE)

        var currentY = hudY + 30

        drawHUDLine(graphics, "SCORE", hudX + 10, currentY)
        currentY += HUD_LINE_HEIGHT
        drawHUDLine(graphics, scoreRegistry.totalPoints.toInt().toString(), hudX + 10, currentY)

        currentY += HUD_LINE_HEIGHT
        drawHUDLine(graphics, "LINES", hudX + 10, currentY)
        currentY += HUD_LINE_HEIGHT
        drawHUDLine(graphics, scoreRegistry.totalLinesCleared.toString(), hudX + 10, currentY)

        currentY += HUD_LINE_HEIGHT
        drawHUDLine(graphics, "TIME", hudX + 10, currentY)
        currentY += HUD_LINE_HEIGHT
        drawHUDLine(graphics, formatTime(tetrisEngine.sessionTimeSeconds), hudX + 10, currentY)
    }

    private fun drawHUDLine(graphics: Graphics2D, text: String, x: Int, y: Int) {
        graphics.drawString(text, x, y)
    }

    private fun drawMessages(graphics: Graphics2D, snapshot: GameSnapshot<T>) {
        if (activeMessage.isNullOrEmpty()) return

        val elapsedTime = System.currentTimeMillis() - messageStartTime

        if (elapsedTime < MESSAGE_DURATION_MS) {
            messageAlpha = 1.0f - (elapsedTime.toFloat() / MESSAGE_DURATION_MS)
            drawCenteredMessage(graphics, snapshot, activeMessage!!)
        } else {
            activeMessage = null
        }
    }

    private fun drawCenteredMessage(graphics: Graphics2D, snapshot: GameSnapshot<T>, message: String) {
        val fontSize = (blockSize * 0.8f).toInt()
        graphics.font = Font(HUD_FONT, Font.BOLD, fontSize)

        val metrics = graphics.fontMetrics
        val boardPixelWidth = snapshot.board.cols * blockSize
        val boardPixelHeight = snapshot.board.rows * blockSize
        val boardOffsetX = BOARD_OFFSET_COLS * blockSize

        val centerX = boardOffsetX + (boardPixelWidth / 2)
        val centerY = boardPixelHeight / 2

        val textX = centerX - (metrics.stringWidth(message) / 2)
        val textY = centerY + (metrics.height / 4)

        val alphaValue = (messageAlpha * 255).toInt()
        graphics.color = Color(255, 255, 255, alphaValue)
        graphics.drawString(message, textX, textY)
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
        return "%02d:%02d".format(minutes, remainingSeconds)
    }
}