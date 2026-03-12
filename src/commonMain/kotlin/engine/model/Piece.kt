package engine.model

enum class SpinType {
    NONE,
    MINI,
    FULL
}

interface Piece {

    val id: Int

    val shape: Matrix

    val name: String

    fun getRotationCenter(): Pair<Int, Int>

    fun getRotationsState(rotationState: Int): Matrix

    fun getKickTable(rotation: Rotation, rotationState: Int): List<Pair<Int, Int>>

    fun getSpinType(board: Board, row: Int, col: Int, rotationState: Int, kickIndex: Int = 0): SpinType = SpinType.NONE
}