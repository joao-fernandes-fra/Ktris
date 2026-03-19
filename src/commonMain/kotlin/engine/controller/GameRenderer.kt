package engine.controller

import engine.model.GameSnapshot
import engine.model.Piece

interface GameRenderer {
    fun render(state: GameSnapshot?)
}