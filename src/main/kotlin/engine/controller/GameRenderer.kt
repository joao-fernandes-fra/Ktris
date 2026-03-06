package engine.controller

import engine.model.GameSnapshot
import engine.model.Piece

interface GameRenderer<T : Piece> {
    suspend fun render(state: GameSnapshot<T>)
}