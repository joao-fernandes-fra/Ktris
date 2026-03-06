package engine.model.defaults

import kotlinx.serialization.Serializable
import engine.model.Board
import engine.model.Matrix
import engine.model.Piece
import engine.model.Rotation
import engine.model.SpinType
import kotlin.math.floor

@Serializable
open class ProceduralPiece(
    override val id: Int,
    override val shape: Matrix,
    override val name: String,
    protected val kicks: SRSKicks = SRSKicks.STANDARD,
) : Piece {
    override fun getRotationCenter(): Pair<Int, Int> {
        return Pair(1, 1)
    }

    override fun getRotationsState(rotationState: Int): Matrix {
        val turns = floor((rotationState % 4).toDouble()).toInt()
        val current = shape.copy()
        repeat(turns) {
            current.transpose()
            current.reverseRows()
        }
        return current
    }

    override fun getKickTable(rotation: Rotation, rotationState: Int): List<Pair<Int, Int>> {
        return when (rotation) {
            Rotation.ROTATE_CW -> kicks.cw[rotationState]
            Rotation.ROTATE_CCW -> kicks.ccw[rotationState]
            Rotation.ROTATE_180 -> kicks._180[rotationState]
        }
    }
}

@Serializable
class ProceduralIPiece(var _id: Int, var _shape: Matrix, var _name: String) : ProceduralPiece(_id, _shape, _name,
    SRSKicks.I_PIECE
) {
    override fun getRotationCenter(): Pair<Int, Int> {
        return Pair(1, 2)
    }
}

@Serializable
class ProceduralTPiece(var _id: Int, var _shape: Matrix, var _name: String) : ProceduralPiece(_id, _shape, _name) {
    override fun getSpinType(board: Board, row: Int, col: Int, rotationState: Int): SpinType {
        val (centerRelRow, centerRelCol) = getRotationCenter()

        // Calculate the absolute board coordinates of the center
        val centerRow = row + centerRelRow
        val centerCol = col + centerRelCol

        // Define corners relative to the center
        // 0: TL, 1: TR, 2: BR, 3: BL
        val corners = listOf(
            centerRow - 1 to centerCol - 1, // 0: Top-Left
            centerRow - 1 to centerCol + 1, // 1: Top-Right
            centerRow + 1 to centerCol + 1, // 2: Bottom-Right
            centerRow + 1 to centerCol - 1  // 3: Bottom-Left
        )

        // Check if these board coordinates are occupied
        val occupied = corners.map { (r, c) -> board.isOccupied(r, c) }
        val occupiedCount = occupied.count { it }

        // 3+ corners required for any T-Spin
        if (occupiedCount < 3) return SpinType.NONE

        //Map the front corners based on rotation (0:Up, 1:Right, 2:Down, 3:Left)
        val frontCorners = when (rotationState) {
            0 -> listOf(0, 1) // Pointing UP: Top-Left and Top-Right are front
            1 -> listOf(1, 2) // Pointing RIGHT: Top-Right and Bottom-Right are front
            2 -> listOf(2, 3) // Pointing DOWN: Bottom-Right and Bottom-Left are front
            3 -> listOf(0, 3) // Pointing LEFT: Top-Left and Bottom-Left are front
            else -> listOf()
        }

        // A Full T-Spin must have both front corners blocked
        val frontBlocked = frontCorners.all { occupied[it] }

        return if (occupiedCount == 4 || frontBlocked) {
            SpinType.FULL
        } else {
            SpinType.MINI
        }
    }

    private fun Board.isOccupied(row: Int, col: Int, emptyValue: Int = 0): Boolean {
        if (row !in 0..<rows || col < 0 || col >= cols) return true
        return this[row, col] != emptyValue
    }
}