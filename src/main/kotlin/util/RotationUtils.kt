package util

import model.Matrix
import model.Rotation

object RotationUtils {
    fun getRotatedCopy(original: Matrix<Int>, rotation: Rotation): Matrix<Int> {
        val newMatrix = original.copy()
        when (rotation) {
            Rotation.ROTATE_CW -> {
                newMatrix.transpose()
                newMatrix.reverseRows()
            }
            Rotation.ROTATE_CCW -> {
                newMatrix.reverseRows()
                newMatrix.transpose()
            }
            Rotation.ROTATE_180 -> {
                newMatrix.flipRows()
                newMatrix.reverseRows()
            }
        }

        return newMatrix
    }
}