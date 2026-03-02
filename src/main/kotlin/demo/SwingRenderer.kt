package demo

import controller.GameRenderer
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

class SwingRenderer<T : Piece>(
    private val scoreRegistry: ScoreRegistry,
    private val boardWidth: Int,
    private val boardHeight: Int,
    eventBus: GameEventBus
) : JPanel(), GameRenderer<T> {

    private var lastSnapshot: GameSnapshot<T>? = null
    private var blockSize = 30
    private var flashAlpha: Float = 0f
    private val flashDuration = 200L // 200ms duration
    private var lastClearTime = 0L

    // In SwingRenderer
    private var activeMoveMessage: String? = null
    private var messageAlpha: Float = 0f
    private val messageDuration = 1500L // 1.5 seconds visibility
    private var messageStartTime = 0L

    init {
        preferredSize = Dimension((boardWidth + 10) * blockSize, boardHeight * blockSize)
        background = Color.BLACK

        eventBus.subscribe<GameEvent.LineCleared> {
            if (it.linesCleared > 0) {
                flashAlpha = 1.0f
                lastClearTime = System.currentTimeMillis()
            }
        }

        eventBus.subscribe<GameEvent.ScoreUpdated> { event ->
            if (event.moveType.isSpecial){
                activeMoveMessage = event.moveType.displayName
                messageAlpha = 1.0f
                messageStartTime = System.currentTimeMillis()
            }
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

        if (activeMoveMessage != null && activeMoveMessage?.isNotEmpty() == true) {
            drawMoveNotification(g2)
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

        for (r in 0 until snapshot.board.rows) {
            for (c in 0 until snapshot.board.cols) {
                val boardBlockId = snapshot.board[r, c]
                val baseColor = getTetrominoColor(boardBlockId ?: 0)
                if (boardBlockId != 0) {
                    drawBlock(g2, r, c, baseColor, boardOffsetX, 0)
                }
            }
        }

        snapshot.ghostPiece?.let { drawPiece(g2, it, boardOffsetX, 0, 30) }

        snapshot.currentPiece?.let { drawPiece(g2, it, boardOffsetX, 0) }

        snapshot.holdPiece?.let { drawPiece(g2, it.toPieceState(), 0, 0) }

        snapshot.nextPieces.forEachIndexed { index, piece ->
            drawPiece(g2, piece!!.toPieceState(), (boardWidth + 6) * blockSize, index * 4 * blockSize)
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

    private fun Color.withAlpha(alpha: Int): Color {
        return Color(this.red, this.green, this.blue, alpha)
    }

    private fun drawMoveNotification(g2: Graphics2D) {
        val snapshot = lastSnapshot ?: return
        val message = activeMoveMessage ?: return
        val elapsed = System.currentTimeMillis() - messageStartTime

        if (elapsed < messageDuration) {
            messageAlpha = 1.0f - (elapsed.toFloat() / messageDuration)

            g2.font = Font("Monospaced", Font.BOLD, (blockSize * 0.8f).toInt())
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
            activeMoveMessage = null
        }
    }

    private fun drawHUD(g2: Graphics2D) {
        val hudX = 10
        val hudY = 5 * blockSize

        g2.font = Font("Monospaced", Font.BOLD, 18)
        g2.color = Color.WHITE

        g2.color = Color(0, 0, 0, 150)
        g2.fillRect(hudX, hudY, 150, 100)

        g2.color = Color.WHITE
        g2.drawString("SCORE", hudX + 10, hudY + 30)
        g2.drawString(scoreRegistry.totalPoints.toInt().toString(), hudX + 10, hudY + 55)

        g2.drawString("LINES", hudX + 10, hudY + 80)
        g2.drawString(scoreRegistry.totalLinesCleared.toString(), hudX + 10, hudY + 105)
    }
}