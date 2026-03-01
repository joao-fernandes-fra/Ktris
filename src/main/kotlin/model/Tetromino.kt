package model


object Tetromino {

    val I = ProceduralPiece(
        1,
        Matrix(
            4, 4,
            0, 0, 0, 0,
            0, 0, 0, 0,
            1, 1, 1, 1,
            0, 0, 0, 0
        ), SRSKicks.I_PIECE
    )

    val O = ProceduralPiece(
        2,
        Matrix(
            2, 2,
            2, 2,
            2, 2
        )
    )

    val T = ProceduralTPiece(
        3,
        Matrix(
            3, 3,
            0, 3, 0,
            3, 3, 3,
            0, 0, 0
        )
    )

    val S = ProceduralPiece(
        4,
        Matrix(
            3, 3,
            0, 4, 4,
            4, 4, 0,
            0, 0, 0
        )
    )

    val Z = ProceduralPiece(
        5,
        Matrix(
            3, 3,
            5, 5, 0,
            0, 5, 5,
            0, 0, 0
        )
    )

    val J = ProceduralPiece(
        6,
        Matrix(
            3, 3,
            6, 0, 0,
            6, 6, 6,
            0, 0, 0
        )
    )

    val L = ProceduralPiece(
        7,
        Matrix(
            3, 3,
            0, 0, 7,
            7, 7, 7,
            0, 0, 0
        )
    )

    val values = listOf(I, O, T, S, Z, J, L)
}