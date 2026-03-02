package demo

import controller.GameRenderer
import controller.TetrisEngine
import controller.defaults.ScoreRegistry
import model.GameEvent
import model.GameEventBus
import model.GameSnapshot
import model.Piece
import model.PieceState
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

/**
 * Renderer built with the assistance of AI [Copilot for this particular iteration]
 */
class SwingRenderer<T : Piece>(
    private val scoreRegistry: ScoreRegistry,
    private val tetrisEngine: TetrisEngine<*>,
    eventBus: GameEventBus,
) : JPanel(), GameRenderer<T> {

    companion object {
        private const val SCREEN_HEIGHT = 720
    }

    private var lastSnapshot: GameSnapshot<T>? = null
    private var blockSize = 30
    private var flashAlpha: Float = 0f
    private val flashDuration = 200L
    private var lastClearTime = 0L

    private var activeMessage: String? = null
    private var messageAlpha: Float = 0f
    private var messageDuration = 1500L
    private var messageStartTime = 0L

    init {
        preferredSize = Dimension(SCREEN_HEIGHT * 4 / 3, SCREEN_HEIGHT)
        background = Color.BLACK

        eventBus.subscribe<GameEvent.LineCleared> {
            if (it.linesCleared > 0) {
                flashAlpha = 1.0f
                lastClearTime = System.currentTimeMillis()
            }
        }

        eventBus.subscribe<GameEvent.ScoreUpdated> { event ->
            if (event.moveType.isSpecial) {
                setMessage(event.moveType.displayName)
            } else if (event.comboCount > 0) {
                setMessage("COMBO x${event.comboCount}")
            } else if (event.backToBackCount > 0) {
                setMessage("BACK TO BACK x${event.backToBackCount}")
            }
        }

        eventBus.subscribe<GameEvent.GarbageSent> { _ ->
            setMessage("GARBAGE INCOMING!")
        }
    }

    private fun setMessage(message: String) {
        activeMessage = message
        messageAlpha = 1.0f
        messageStartTime = System.currentTimeMillis()
    }

    private fun getTetrominoColor(id: Int): Color = when (id) {
        1 -> Color.CYAN
        2 -> Color.YELLOW
        3 -> Color.MAGENTA
        4 -> Color.GREEN
        5 -> Color.RED
        6 -> Color.BLUE
        7 -> Color.ORANGE
        else -> Color.LIGHT_GRAY
    }

    override fun render(state: GameSnapshot<T>) {
        this.lastSnapshot = state
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        val snapshot = lastSnapshot ?: return

        renderClearEffect(g2)
        renderHUD(g2)

        val padding = 12
        val totalRows = snapshot.board.rows
        val availableWidth = width / (snapshot.board.cols + padding)
        val availableHeight = height / (totalRows + 2)
        blockSize = min(availableWidth, availableHeight)

        val marginY = blockSize
        val boardOffsetX = 5 * blockSize

        val bufferRows = snapshot.board.bufferHeight
        val visibleRows = (snapshot.board.rows - bufferRows).coerceAtLeast(1)

        val boardPixelWidth = snapshot.board.cols * blockSize
        val boardPixelHeight = visibleRows * blockSize

        val bottomY = marginY + boardPixelHeight

        val xLeft = boardOffsetX - 2
        val xRight = boardOffsetX + boardPixelWidth + 2

        g2.color = Color.WHITE
        g2.stroke = BasicStroke(5f)
        g2.drawLine(xLeft, marginY, xLeft, bottomY)
        g2.drawLine(xRight, marginY, xRight, bottomY)
        g2.drawLine(xLeft, bottomY, xRight, bottomY)

        renderBoard(snapshot, g2, boardOffsetX, marginY, bufferRows)

        snapshot.ghostPiece?.let { drawPieceOnBoard(g2, it, boardOffsetX, marginY, bufferRows, 30) }
        snapshot.currentPiece?.let { drawPieceOnBoard(g2, it, boardOffsetX, marginY, bufferRows) }

        val previewPadding = blockSize / 2
        val holdBoxX = boardOffsetX - (4 * blockSize) - (previewPadding * 2)
        val nextBoxX = boardOffsetX + boardPixelWidth + previewPadding

        g2.color = Color(0, 0, 0, 120)
        g2.fillRect(holdBoxX - previewPadding,
            marginY - previewPadding, 4 * blockSize + previewPadding * 2, 4 * blockSize + previewPadding * 2)

        g2.fillRect(nextBoxX - previewPadding,
            marginY - previewPadding, 4 * blockSize + previewPadding * 2, (snapshot.nextPieces.size * 4 * blockSize) + previewPadding * 2)

        snapshot.holdPiece?.let {
            drawPiecePreview(g2, it.toPieceState(), holdBoxX, marginY)
        }

        snapshot.nextPieces.forEachIndexed { index, piece ->
            piece?.let {
                val py = marginY + index * 4 * blockSize
                drawPiecePreview(g2, it.toPieceState(), nextBoxX, py)
            }
        }

        if (!activeMessage.isNullOrEmpty()) {
            drawMoveNotification(snapshot, g2, marginY, boardPixelWidth, bufferRows)
        }
    }

    private fun renderBoard(
        snapshot: GameSnapshot<T>,
        g2: Graphics2D,
        boardOffsetX: Int,
        boardOffsetY: Int,
        bufferRows: Int
    ) {
        for (row in 0 until snapshot.board.rows) {
            for (col in 0 until snapshot.board.cols) {
                val boardBlockId = snapshot.board[row, col] ?: 0
                if (boardBlockId == 0) continue

                val visibleRow = row - bufferRows
                if (visibleRow < 0) continue

                if (boardBlockId == -1) {
                    drawGlowingBlock(g2, visibleRow, col, boardOffsetX, boardOffsetY)
                } else {
                    drawBlock(g2, visibleRow, col, getTetrominoColor(boardBlockId), boardOffsetX, boardOffsetY)
                }
            }
        }
    }

    private fun renderClearEffect(g2: Graphics2D) {
        val elapsed = System.currentTimeMillis() - lastClearTime
        if (elapsed < flashDuration) {
            flashAlpha = 1.0f - (elapsed.toFloat() / flashDuration)
            val alpha = (flashAlpha * 128).toInt().coerceIn(0, 255)
            g2.color = Color(255, 255, 255, alpha)
            g2.fillRect(0, 0, width, height)
        }
    }

    private fun drawPieceOnBoard(
        g2: Graphics2D,
        piece: PieceState<T>,
        offsetX: Int,
        offsetY: Int,
        bufferRows: Int,
        opacity: Int = 100
    ) {
        val baseColor = getTetrominoColor(piece.type.id)
        val alpha = (opacity * 255 / 100).coerceIn(0, 255)
        val color = baseColor.withAlpha(alpha)

        for (r in 0 until piece.shape.rows) {
            for (c in 0 until piece.shape.cols) {
                if (piece.shape[r, c] != 0) {
                    val boardRow = piece.row + r - bufferRows
                    if (boardRow < 0) continue
                    drawBlock(g2, boardRow, piece.col + c, color, offsetX, offsetY)
                }
            }
        }
    }

    private fun drawPiecePreview(
        g2: Graphics2D,
        piece: PieceState<T>,
        previewOriginX: Int,
        previewOriginY: Int
    ) {
        val baseColor = getTetrominoColor(piece.type.id)
        for (r in 0 until piece.shape.rows) {
            for (c in 0 until piece.shape.cols) {
                if (piece.shape[r, c] != 0) {
                    drawBlock(g2, r, c, baseColor, previewOriginX, previewOriginY)
                }
            }
        }
    }

    private fun drawBlock(g2: Graphics2D, r: Int, c: Int, color: Color, offsetX: Int, offsetY: Int) {
        if (r < 0) return
        if (c < 0) return
        g2.color = color
        g2.fillRect(offsetX + c * blockSize, offsetY + r * blockSize, blockSize - 2, blockSize - 2)
    }

    private fun drawGlowingBlock(g2: Graphics2D, r: Int, c: Int, offsetX: Int, offsetY: Int) {
        if (r < 0) return
        val x = offsetX + c * blockSize
        val y = offsetY + r * blockSize

        val pulse = (sin(System.currentTimeMillis() / 150.0) * 0.25 + 0.55).toFloat()
        val alphaOuter = (pulse * 100).toInt().coerceIn(0, 255)

        g2.color = Color(255, 255, 255, alphaOuter)
        g2.fillRect(x - 2, y - 2, blockSize + 2, blockSize + 2)

        g2.color = Color.WHITE
        g2.fillRect(x, y, blockSize - 2, blockSize - 2)

        g2.stroke = BasicStroke(1f)
        g2.color = Color(255, 255, 255, 200)
        g2.drawRect(x + 2, y + 2, blockSize - 6, blockSize - 6)
    }

    private fun Color.withAlpha(alpha: Int): Color {
        val a = alpha.coerceIn(0, 255)
        return Color(this.red, this.green, this.blue, a)
    }

    private fun drawMoveNotification(snapshot: GameSnapshot<*>, g2: Graphics2D, boardTopY: Int, boardPixelWidth: Int, bufferRows: Int) {
        val message = activeMessage ?: return
        val elapsed = System.currentTimeMillis() - messageStartTime

        if (elapsed < messageDuration) {
            messageAlpha = 1.0f - (elapsed.toFloat() / messageDuration)

            g2.font = Font("Sanserif", Font.BOLD, (blockSize * 0.8f).toInt())
            val metrics = g2.fontMetrics

            val boardOffsetX = 5 * blockSize
            val centerX = boardOffsetX + (boardPixelWidth / 2)
            val centerY = boardTopY + ((snapshot.board.rows - bufferRows) * blockSize) / 2

            val x = centerX - (metrics.stringWidth(message) / 2)
            val y = centerY + (metrics.height / 4)

            val alpha = (messageAlpha * 255).toInt().coerceIn(0, 255)
            g2.color = Color(255, 255, 255, alpha)
            g2.drawString(message, x, y)
        } else {
            activeMessage = null
        }
    }

    private fun renderHUD(g2: Graphics2D) {
        val hudX = 10
        val hudY = 5 * blockSize
        val lineHeight = 25

        g2.font = Font("Monospaced", Font.BOLD, 18)

        g2.color = Color(0, 0, 0, 150)
        g2.fillRect(hudX, hudY, 150, 150)

        g2.color = Color.WHITE

        var currentY = hudY + 30

        g2.drawString("SCORE", hudX + 10, currentY)
        currentY += lineHeight
        g2.drawString(scoreRegistry.totalPoints.toInt().toString(), hudX + 10, currentY)

        currentY += lineHeight

        g2.drawString("LINES", hudX + 10, currentY)
        currentY += lineHeight
        g2.drawString(scoreRegistry.totalLinesCleared.toString(), hudX + 10, currentY)

        currentY += lineHeight

        g2.drawString("TIME", hudX + 10, currentY)
        currentY += lineHeight
        g2.drawString(formatTime(tetrisEngine.sessionTimeSeconds), hudX + 10, currentY)
    }

    fun formatTime(seconds: Float): String {
        val totalSeconds = seconds.toInt()
        val minutes = (totalSeconds / 60) % 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}