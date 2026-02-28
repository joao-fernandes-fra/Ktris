package util

import model.Matrix

fun Matrix<Int>.stamp(shape: Matrix<Int>, startR: Int, startC: Int, overrideValue: Int) {
    for (r in 0 until shape.rows) {
        for (c in 0 until shape.cols) {
            if (shape[r, c] != 0) {
                val tr = startR + r
                val tc = startC + c
                if (tr in 0 until this.rows && tc in 0 until this.cols) {
                    this[tr, tc] = overrideValue
                }
            }
        }
    }
}