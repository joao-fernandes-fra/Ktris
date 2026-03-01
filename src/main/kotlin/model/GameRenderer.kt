package model

interface GameRenderer<T : Piece> {
    fun render(state: GameSnapshot<T>)
}