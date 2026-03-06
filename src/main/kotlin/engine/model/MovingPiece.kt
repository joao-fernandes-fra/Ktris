package engine.model

interface MovingPiece<T : Piece> {
    var piece: T
    var pieceRow: Int
    var pieceCol: Int
    var rotationState: Int
    var shape: Matrix
    fun move(tagetRow: Int, tagetCol: Int)
    fun projectRotation(rotation: Rotation): Pair<Matrix, Int>
    fun rotateShape(rotatedShape: Matrix, newRow: Int, newCol: Int, rotation: Rotation)
}