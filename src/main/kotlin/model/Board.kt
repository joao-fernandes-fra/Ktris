package model

data class Board(
    var contents: Matrix<Int>,
    var bufferSize: Int = 0,
) {
    constructor(rows: Int, cols: Int, bufferedHeight: Int, initialValue: Int = 0) : this(Matrix(rows, cols, initialValue), bufferedHeight)
    constructor(rows: Int, cols: Int, initialValue: Int = 0) : this(Matrix(rows, cols, initialValue))

    operator fun get(row: Int, col: Int): Int? = contents[row, col]
    operator fun set(row: Int, col: Int, value: Int?){ contents[row, col] = value }

    val rows get() = contents.rows
    val cols get() = contents.cols

    val isEmpty: Boolean get() = contents.isEmpty(0)
}