package engine.model.defaults

import engine.model.Board
import engine.model.Matrix
import engine.model.Piece
import engine.model.Rotation
import engine.model.SpinType
import kotlin.math.floor

open class ProceduralPiece(
    override val id: Int,
    override val shape: Matrix,
    override val name: String,
    protected val kicks: SRSKicks = SRSKicks.STANDARD,
) : Piece {
    override val isSpinEligible: Boolean = false
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

    override fun getSpinType(board: Board, row: Int, col: Int, rotationState: Int, kickIndex: Int): SpinType {
        if (kickIndex == 0) return SpinType.NONE

        val currentShape = getRotationsState(rotationState)
        val blockedDirections = listOf(
            0 to -1, 0 to 1, -1 to 0, 1 to 0
        ).count { (dRow, dCol) -> isBlocked(board, currentShape, row + dRow, col + dCol) }

        return when {
            blockedDirections >= 3 -> SpinType.FULL
            blockedDirections == 2 -> SpinType.MINI
            else -> SpinType.NONE
        }
    }

    private fun isBlocked(board: Board, shape: Matrix, row: Int, col: Int): Boolean {
        for (r in 0 until shape.rows) {
            for (c in 0 until shape.cols) {
                if (shape[r, c] == 0) continue
                val br = row + r
                val bc = col + c
                if (br !in 0 until board.rows || bc !in 0 until board.cols) return true
                if (board[br, bc] != 0) return true
            }
        }
        return false
    }
}

class ProceduralIPiece(var _id: Int, var _shape: Matrix, var _name: String) : ProceduralPiece(
    _id, _shape, _name, SRSKicks.I_PIECE
) {
    override val isSpinEligible: Boolean = false
    override fun getRotationCenter(): Pair<Int, Int> {
        return Pair(1, 2)
    }

    override fun getSpinType(board: Board, row: Int, col: Int, rotationState: Int, kickIndex: Int): SpinType {
        return SpinType.NONE
    }
}

class ProceduralTPiece(_id: Int, _shape: Matrix, _name: String) : ProceduralPiece(_id, _shape, _name) {
    override val isSpinEligible: Boolean = true
    override fun getSpinType(board: Board, row: Int, col: Int, rotationState: Int, kickIndex: Int): SpinType {
        val (centerRelRow, centerRelCol) = getRotationCenter()
        val centerRow = row + centerRelRow
        val centerCol = col + centerRelCol

        val corners = listOf(
            centerRow - 1 to centerCol - 1, // 0: TL
            centerRow - 1 to centerCol + 1, // 1: TR
            centerRow + 1 to centerCol + 1, // 2: BR
            centerRow + 1 to centerCol - 1  // 3: BL
        )

        val occupied = corners.map { (r, c) -> board.isOccupied(r, c) }
        val occupiedCount = occupied.count { it }

        if (occupiedCount < 3) return SpinType.NONE

        val frontCorners = when (rotationState) {
            0 -> listOf(0, 1)
            1 -> listOf(1, 2)
            2 -> listOf(2, 3)
            3 -> listOf(0, 3)
            else -> return SpinType.NONE
        }

        val frontBlocked = frontCorners.all { occupied[it] }

        // ← FULL with all 4 corners or both front corners blocked doesn't need a kick
        // ← MINI requires a kick — natural sliding into a 3-corner position isn't a spin
        return when {
            occupiedCount == 4 || frontBlocked -> SpinType.FULL
            kickIndex > 0 -> SpinType.MINI
            else -> SpinType.NONE
        }
    }
}