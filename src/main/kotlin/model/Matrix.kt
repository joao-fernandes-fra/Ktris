package model

data class Matrix<T>(
    val rows: Int,
    val cols: Int,
) {
    private val data: Array<T?> = arrayOfNulls<Any>(rows * cols) as Array<T?>

    val dimension = cols * rows

    constructor(rows: Int, cols: Int, initialValue: T) : this(rows, cols) {
        fill(initialValue)
    }

    constructor(rows: Int, cols: Int, vararg values: T) : this(rows, cols) {
        values.copyInto(data)
    }

    fun fill(value: T) {
        for (r in rows - 1 downTo 0) {
            for (c in cols - 1 downTo 0) {
                data[r * cols + c] = value
            }
        }
    }

    operator fun get(row: Int, col: Int): T? = data[row * cols + col]
    operator fun set(row: Int, col: Int, value: T?) {
        data[row * cols + col] = value
    }

    fun transpose(): Matrix<T> {
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

    fun flipRows(): Matrix<T> {
        for (r in 0 until rows / 2) {
            for (c in 0 until cols) {
                val temp = this[r, c]
                this[r, c] = this[rows - 1 - r, c]
                this[rows - 1 - r, c] = temp
            }
        }
        return this
    }

    fun reverseRows(): Matrix<T> {
        for (r in 0 until rows) {
            for (c in 0 until cols / 2) {
                val temp = this[r, c]
                this[r, c] = this[r, cols - 1 - c]
                this[r, cols - 1 - c] = temp
            }
        }
        return this
    }

    fun copy(): Matrix<T> {
        val newMatrix = Matrix<T>(rows, cols)
        for (i in 0 until dimension) {
            newMatrix.data[i] = this.data[i]
        }
        return newMatrix
    }

    fun isEmpty(emptyValue: T? = null): Boolean {
        for (r in 0..<rows)
            for (c in 0..<cols)
                if (emptyValue != this[r, c])
                    return false
        return true
    }

    fun getRow(row: Int): List<T> {
        val result = ArrayList<T>()
        for (c in 0..<cols) {
            val value = this[row, c] ?: return result
            result.add(value)
        }
        return result
    }
}