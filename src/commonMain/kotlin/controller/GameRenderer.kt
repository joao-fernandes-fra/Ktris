package controller

import model.GameSnapshot
import model.Piece

interface GameRenderer<T : Piece> {
    fun render(state: GameSnapshot<T>)
}