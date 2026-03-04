package model.defaults

import kotlinx.serialization.Serializable
import model.Board
import model.Matrix
import model.Piece
import model.Rotation
import model.SpinType

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
        val turns = Math.floorMod(rotationState, 4)
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
        val (centerX, centerY) = getRotationCenter()

        val corners = listOf(
            centerX - 1 to centerY - 1, // Top-Left
            centerX - 1 to centerY + 1, // Top-Right
            centerX + 1 to centerY + 1, // Bottom-Right
            centerX + 1 to centerY - 1  // Bottom-Left
        )
        val occupied = corners.map { (r, c) -> board.isOccupied(r, c) }
        val occupiedCount = occupied.count { it }

        if (occupiedCount < 3) return SpinType.NONE

        // Determine which corners are the "Front" (facing direction of the T-tip)
        // 0: Up, 1: Right, 2: Down, 3: Left
        val frontCorners = when (rotationState) {
            0 -> listOf(0, 1) // Pointing UP (Top corners are front)
            1 -> listOf(1, 3) // Pointing RIGHT (Right corners are front)
            2 -> listOf(2, 3) // Pointing DOWN (Bottom corners are front)
            3 -> listOf(0, 2) // Pointing LEFT (Left corners are front)
            else -> listOf()
        }

        // A regular T-Spin must have both front corners blocked.
        // 4 corners filled is ALWAYS a Regular T-Spin.
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