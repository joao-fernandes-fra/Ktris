package model.defaults

import model.Matrix
import model.MovingPiece
import model.Piece
import model.Rotation


typealias Shape = Matrix<Int>
typealias RotationState = Pair<Shape, Int>

data class DefaultMovingPiece<T : Piece>(
    override var piece: T,
    override var pieceRow: Int = 0,
    override var pieceCol: Int = 0,
    override var rotationState: Int = 0,
    override var shape: Shape = piece.shape.copy(),
) : MovingPiece<T> {
    override fun move(tagetRow: Int, tagetCol: Int) {
        pieceRow = tagetRow
        pieceCol = tagetCol
    }

    override fun projectRotation(rotation: Rotation): RotationState {
        val rotationState = calculateNextState(rotation)
        return piece.getRotationsState(rotationState) to rotationState
    }


    override fun rotateShape(rotatedShape: Matrix<Int>, newRow: Int, newCol: Int, rotation: Rotation) {
        rotationState = calculateNextState(rotation)
        shape = rotatedShape
        pieceRow = newRow
        pieceCol = newCol
    }

    private fun calculateNextState(rotation: Rotation): Int = when (rotation) {
        Rotation.ROTATE_CW -> (rotationState + 1) % 4
        Rotation.ROTATE_CCW -> (rotationState + 3) % 4
        Rotation.ROTATE_180 -> (rotationState + 2) % 4
    }
}