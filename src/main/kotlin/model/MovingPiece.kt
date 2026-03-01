package model


typealias Shape = Matrix<Int>
typealias RotationState = Pair<Shape, Int>

data class MovingPiece<T : Piece>(
    var piece: T,
    var pieceRow: Int = 0,
    var pieceCol: Int = 0,
    var rotationState: Int = 0,
    var shape: Shape = piece.shape.copy(),
) {
    fun move(tagetRow: Int, tagetCol: Int) {
        pieceRow = tagetRow
        pieceCol = tagetCol
    }

    fun projectRotation(rotation: Rotation): RotationState {
        val rotationState = calculateNextState(rotation)
        return piece.getRotationsState(rotationState) to rotationState
    }


    fun rotateShape(rotatedShape: Matrix<Int>, newRow: Int, newCol: Int, rotation: Rotation) {
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