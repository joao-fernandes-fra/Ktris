package model

import kotlinx.serialization.Serializable

@Serializable
data class Matrix(
    val rows: Int,
    val cols: Int,
) {
    private val data: IntArray = IntArray(rows * cols) { 0 }

    val dimension = cols * rows

    constructor(rows: Int, cols: Int, initialValue: Int) : this(rows, cols) {
        fill(initialValue)
    }

    constructor(rows: Int, cols: Int, vararg values: Int) : this(rows, cols) {
        values.copyInto(data)
    }

    fun fill(value: Int) {
        for (r in rows - 1 downTo 0) {
            for (c in cols - 1 downTo 0) {
                data[r * cols + c] = value
            }
        }
    }

    operator fun get(row: Int, col: Int): Int = data[row * cols + col]
    operator fun set(row: Int, col: Int, value: Int) {
        data[row * cols + col] = value
    }

    fun transpose(): Matrix {
        if (rows != cols) return this
        for (r in 0 until rows) {
            for (c in r + 1 until cols) {
                val temp = this[r, c]
                this[r, c] = this[c, r]
                this[c, r] = temp
            }
        }
        return this
    }

    fun flipRows(): Matrix {
        for (r in 0 until rows / 2) {
            for (c in 0 until cols) {
                val temp = this[r, c]
                this[r, c] = this[rows - 1 - r, c]
                this[rows - 1 - r, c] = temp
            }
        }
        return this
    }

    fun reverseRows(): Matrix {
        for (r in 0 until rows) {
            for (c in 0 until cols / 2) {
                val temp = this[r, c]
                this[r, c] = this[r, cols - 1 - c]
                this[r, cols - 1 - c] = temp
            }
        }
        return this
    }

    fun copy(): Matrix {
        val newMatrix = Matrix(rows, cols)
        for (i in 0 until dimension) {
            newMatrix.data[i] = this.data[i]
        }
        return newMatrix
    }

    fun isEmpty(): Boolean {
        for (r in 0..<rows)
            for (c in 0..<cols)
                if (0 != this[r, c])
                    return false
        return true
    }

    fun getRow(row: Int): List<Int> {
        val result = ArrayList<Int>()
        for (c in 0..<cols) {
            val value = this[row, c] ?: return result
            result.add(value)
        }
        return result
    }
}