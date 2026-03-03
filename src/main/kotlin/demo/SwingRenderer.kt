package demo

import controller.GameRenderer
import controller.TetrisEngine
import controller.defaults.ScoreRegistry
import model.Board
import model.GameEvent
import model.GameEventBus
import model.GameSnapshot
import model.Piece
import model.PieceState
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JPanel

class SwingRenderer<T : Piece>(
    private val scoreRegistry: ScoreRegistry,
    private val tetrisEngine: TetrisEngine<*>,
    eventBus: GameEventBus
) : JPanel(), GameRenderer<T> {

    companion object {
        private const val SCREEN_HEIGHT = 720
    }

    private var snapshot: GameSnapshot<T>? = null

    init {
        preferredSize = Dimension(SCREEN_HEIGHT * 16 / 9, SCREEN_HEIGHT)
        background = Color.BLACK

        eventBus.subscribe<GameEvent.LineCleared> {

        }

        eventBus.subscribe<GameEvent.ScoreUpdated> { event ->

        }

        eventBus.subscribe<GameEvent.GarbageSent> { event ->

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
        this.snapshot = state
        repaint()
    }

    override fun paintComponent(g: Graphics) {

    }

    private fun drawPiece(board: Board, g2: Graphics2D, piece: PieceState<T>, offsetX: Int, offsetY: Int, opacity: Int = 100) {
        val baseColor = getTetrominoColor(piece.type.id)

        val alpha = (opacity * 255 / 100).coerceIn(0, 255)
        val color = baseColor.withAlpha(alpha)
        val actualRow = piece.row + board.bufferSize

        for (r in 0 until piece.shape.rows) {
            for (c in 0 until piece.shape.cols) {
                if (piece.shape[r, c] != 0) {
                    drawBlock(g2, actualRow + r, piece.col + c, color, offsetX, offsetY)
                }
            }
        }
    }

    private fun drawBlock(g2: Graphics2D, r: Int, c: Int, color: Color, offsetX: Int, offsetY: Int) {
        g2.color = color
    }

    private fun Color.withAlpha(alpha: Int): Color {
        return Color(this.red, this.green, this.blue, alpha)
    }
}