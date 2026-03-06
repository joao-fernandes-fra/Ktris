package engine.controller

import engine.model.events.Event

interface InputHandler {
    suspend fun handleInput(input: Event)
}