package model


//TODO: change this fixed enum to an interface implementation where the devs can either use the base mathRotated(this) pieces or define state rotations with multiple matrices so that 180 rotation are possible
enum class Tetromino(val shape: Matrix<Int>) {
    I(Matrix(4, 4,
        0, 0, 0, 0,
        0, 0, 0, 0,
        1, 1, 1, 1,
        0, 0, 0, 0
    )),

    O(Matrix(2, 2,
        2, 2,
        2, 2
    )),

    T(Matrix(3, 3,
        0, 3, 0,
        3, 3, 3,
        0, 0, 0
    )),

    S(Matrix(3, 3,
        0, 4, 4,
        4, 4, 0,
        0, 0, 0
    )),

    Z(Matrix(3, 3,
        5, 5, 0,
        0, 5, 5,
        0, 0, 0
    )),

    J(Matrix(3, 3,
        6, 0, 0,
        6, 6, 6,
        0, 0, 0
    )),

    L(Matrix(3, 3,
        0, 0, 7,
        7, 7, 7,
        0, 0, 0
    ));
}