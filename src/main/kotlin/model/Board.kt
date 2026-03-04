package model

data class Board(
    var contents: Matrix,
    var bufferSize: Int = 0,
) {
    constructor(rows: Int, cols: Int, bufferSize: Int, initialValue: Int = 0) : this(
        Matrix(
            rows + bufferSize,
            cols,
            initialValue
        ), bufferSize
    )

    constructor(rows: Int, cols: Int, initialValue: Int = 0) : this(Matrix(rows, cols, initialValue))

    operator fun get(row: Int, col: Int): Int = contents[row, col]
    operator fun set(row: Int, col: Int, value: Int) {
        contents[row, col] = value
    }

    val rows get() = contents.rows
    val cols get() = contents.cols

    val isEmpty: Boolean get() = contents.isEmpty()
    val visibleRows: Int get() = rows - bufferSize

    override fun toString(): String {
        return (0 until rows).joinToString("\n") { r ->
            (0 until cols)
                .map { c -> this[r, c] ?: 0 }
                .joinToString(prefix = "[", postfix = "]", separator = ",")
        }
    }
}