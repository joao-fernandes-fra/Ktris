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
import kotlin.math.sin

class SwingRenderer<T : Piece>(
    private val scoreRegistry: ScoreRegistry,
    private val tetrisEngine: TetrisEngine<*>,
    eventBus: GameEventBus
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
        preferredSize = Dimension(SCREEN_HEIGHT * 16 / 9, SCREEN_HEIGHT)
        background = Color.BLACK

        eventBus.subscribe<GameEvent.LineCleared> {
            if (it.linesCleared > 0) {
                flashAlpha = 1.0f
                lastClearTime = System.currentTimeMillis()
            }
        }

        eventBus.subscribe<GameEvent.ScoreUpdated> { event ->
            if (event.moveType.isSpecial){
                activeMessage = event.moveType.displayName
                messageAlpha = 1.0f
                messageStartTime = System.currentTimeMillis()
            }
        }

        eventBus.subscribe<GameEvent.GarbageSent> { event ->
            activeMessage = "GARBAGE INCOMING!"
            messageAlpha = 1.0f
            messageStartTime = System.currentTimeMillis()
        }
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

        val padding = 12
        val availableWidth = width / (snapshot.board.cols + padding)
        val availableHeight = height / snapshot.board.rows
        blockSize = minOf(availableWidth, availableHeight)

        val elapsed = System.currentTimeMillis() - lastClearTime
        if (elapsed < flashDuration) {
            flashAlpha = 1.0f - (elapsed.toFloat() / flashDuration)

            g2.color = Color(255, 255, 255, (flashAlpha * 128).toInt())
            g2.fillRect(0, 0, width, height)
        }
        drawHUD(g2)

        val boardOffsetX = 5 * blockSize
        g2.color = Color.WHITE
        g2.stroke = BasicStroke(2f)
        g2.drawRect(
            boardOffsetX - 2,
            -2,
            (snapshot.board.cols * blockSize) + 4,
            (snapshot.board.rows * blockSize) + 4
        )

        for (row in 0 until snapshot.board.rows) {
            for (col in 0 until snapshot.board.cols) {
                val boardBlockId = snapshot.board[row, col]
                val baseColor = getTetrominoColor(boardBlockId ?: 0)
                if (boardBlockId == -1) {
                    drawGlowingBlock(g2, row, col, boardOffsetX, 0)
                } else if (boardBlockId != 0) {
                    drawBlock(g2, row, col, baseColor, boardOffsetX, 0)
                }
            }
        }

        snapshot.ghostPiece?.let { drawPiece(g2, it, boardOffsetX, 0, 30) }

        snapshot.currentPiece?.let { drawPiece(g2, it, boardOffsetX, 0) }

        snapshot.holdPiece?.let { drawPiece(g2, it.toPieceState(), 0, 0) }

        snapshot.nextPieces.forEachIndexed { index, piece ->
            drawPiece(g2, piece!!.toPieceState(), (snapshot.board.cols + 6) * blockSize, index * 4 * blockSize)
        }

        if (activeMessage != null && activeMessage?.isNotEmpty() == true) {
            drawMoveNotification(g2)
        }

    }

    private fun drawPiece(g2: Graphics2D, piece: PieceState<T>, offsetX: Int, offsetY: Int, opacity: Int = 100) {
        val baseColor = getTetrominoColor(piece.type.id)

        val alpha = (opacity * 255 / 100).coerceIn(0, 255)
        val color = baseColor.withAlpha(alpha)

        for (r in 0 until piece.shape.rows) {
            for (c in 0 until piece.shape.cols) {
                if (piece.shape[r, c] != 0) {
                    drawBlock(g2, piece.row + r, piece.col + c, color, offsetX, offsetY)
                }
            }
        }
    }

    private fun drawBlock(g2: Graphics2D, r: Int, c: Int, color: Color, offsetX: Int, offsetY: Int) {
        g2.color = color
        g2.fillRect(offsetX + c * blockSize, offsetY + r * blockSize, blockSize - 2, blockSize - 2)
    }

    private fun drawGlowingBlock(g2: Graphics2D, r: Int, c: Int, offsetX: Int, offsetY: Int) {
        val x = offsetX + c * blockSize
        val y = offsetY + r * blockSize

        val pulse = (sin(System.currentTimeMillis() / 150.0) * 0.25 + 0.55).toFloat()

        g2.color = Color(255, 255, 255, (pulse * 100).toInt())
        g2.fillRect(x - 2, y - 2, blockSize + 2, blockSize + 2)

        g2.color = Color.WHITE
        g2.fillRect(x, y, blockSize - 2, blockSize - 2)

        g2.stroke = BasicStroke(1f)
        g2.color = Color(255, 255, 255, 200)
        g2.drawRect(x + 2, y + 2, blockSize - 6, blockSize - 6)
    }

    private fun Color.withAlpha(alpha: Int): Color {
        return Color(this.red, this.green, this.blue, alpha)
    }

    private fun drawMoveNotification(g2: Graphics2D) {
        val snapshot = lastSnapshot ?: return
        val message = activeMessage ?: return
        val elapsed = System.currentTimeMillis() - messageStartTime

        if (elapsed < messageDuration) {
            messageAlpha = 1.0f - (elapsed.toFloat() / messageDuration)

            g2.font = Font("Sanserif", Font.BOLD, (blockSize * 0.8f).toInt())
            val metrics = g2.fontMetrics

            val boardPixelWidth = snapshot.board.cols * blockSize
            val boardPixelHeight = snapshot.board.rows * blockSize
            val boardOffsetX = 5 * blockSize

            val centerX = boardOffsetX + (boardPixelWidth / 2)
            val centerY = boardPixelHeight / 2

            val x = centerX - (metrics.stringWidth(message) / 2)
            val y = centerY + (metrics.height / 4)

            g2.color = Color(255, 255, 255, (messageAlpha * 255).toInt())
            g2.drawString(message, x, y)
        } else {
            activeMessage = null
        }
    }

    private fun drawHUD(g2: Graphics2D) {
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