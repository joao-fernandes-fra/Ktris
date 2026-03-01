package demo

import model.GameEvent
import model.GameEventBus
import model.GameRenderer
import model.GameSnapshot
import model.PieceState
import model.ScoreRegistry
import model.toPieceState
import java.awt.*
import javax.swing.JPanel

class SwingRenderer(
    private val scoreRegistry: ScoreRegistry,
    private val boardWidth: Int,
    private val boardHeight: Int,
    eventBus: GameEventBus
) : JPanel(), GameRenderer {

    private var lastSnapshot: GameSnapshot? = null
    private val blockSize = 30
    private var flashAlpha: Float = 0f
    private val flashDuration = 200L // 200ms duration
    private var lastClearTime = 0L

    init {
        preferredSize = Dimension((boardWidth + 10) * blockSize, boardHeight * blockSize)
        background = Color.BLACK

        eventBus.subscribe<GameEvent.LineCleared> {
            if (it.linesCleared > 0) {
                flashAlpha = 1.0f
                lastClearTime = System.currentTimeMillis()
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

    override fun render(state: GameSnapshot) {
        this.lastSnapshot = state
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        val snapshot = lastSnapshot ?: return

        val elapsed = System.currentTimeMillis() - lastClearTime
        if (elapsed < flashDuration) {
            flashAlpha = 1.0f - (elapsed.toFloat() / flashDuration)

            g2.color = Color(255, 255, 255, (flashAlpha * 128).toInt())
            g2.fillRect(0, 0, width, height)
        }
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

        g2.color = Color.WHITE
        g2.drawString("Score: ${scoreRegistry.totalPoints}", 10, boardHeight * blockSize - 10)
        g2.drawString("Lines: ${scoreRegistry.totalLinesCleared}", 100, boardHeight * blockSize - 10)
    }

    private fun drawPiece(g2: Graphics2D, piece: PieceState, offsetX: Int, offsetY: Int, opacity: Int = 100) {
        val baseColor = getTetrominoColor(piece.type.ordinal + 1)

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
}