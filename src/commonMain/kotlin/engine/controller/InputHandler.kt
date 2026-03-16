package engine.controller

import engine.model.events.GameEvent

interface InputHandler {
    fun handleInput(input: GameEvent)
}