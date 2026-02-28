package handson

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
    private val boardHeight: Int
) : JPanel(), GameRenderer {

    private var lastSnapshot: GameSnapshot? = null
    private val blockSize = 30

    init {
        preferredSize = Dimension((boardWidth + 10) * blockSize, boardHeight * blockSize)
        background = Color.BLACK
    }

    override fun render(state: GameSnapshot) {
        this.lastSnapshot = state
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        val snapshot = lastSnapshot ?: return

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
                if (snapshot.board[r, c] != 0) {
                    drawBlock(g2, r, c, Color.GRAY, boardOffsetX, 0)
                }
            }
        }

        snapshot.ghostPiece?.let { drawPiece(g2, it, Color.LIGHT_GRAY, boardOffsetX, 0) }

        snapshot.currentPiece?.let { drawPiece(g2, it, Color.GREEN, boardOffsetX, 0) }

        snapshot.holdPiece?.let { drawPiece(g2, it.toPieceState(), Color.LIGHT_GRAY, 0, 0) }

        snapshot.nextPieces.forEachIndexed { index, piece ->
            drawPiece(g2, piece!!.toPieceState(), Color.GREEN, (boardWidth + 6) * blockSize, index * 4 * blockSize)
        }

        g2.color = Color.WHITE
        g2.drawString("Score: ${scoreRegistry.totalPoints}", 10, boardHeight * blockSize - 10)
        g2.drawString("Lines: ${scoreRegistry.totalLinesCleared}", 100, boardHeight * blockSize - 10)
    }

    private fun drawPiece(g2: Graphics2D, piece: PieceState, color: Color, offsetX: Int, offsetY: Int) {
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
}