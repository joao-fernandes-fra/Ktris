package model

import model.defaults.RotationState
import model.defaults.Shape

interface MovingPiece<T : Piece> {
    var piece: T
    var pieceRow: Int
    var pieceCol: Int
    var rotationState: Int
    var shape: Shape
    fun move(tagetRow: Int, tagetCol: Int)
    fun projectRotation(rotation: Rotation): RotationState
    fun rotateShape(rotatedShape: Matrix<Int>, newRow: Int, newCol: Int, rotation: Rotation)
}