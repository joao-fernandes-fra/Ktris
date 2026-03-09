package engine.controller

import engine.model.events.Event

interface InputHandler {
    fun handleInput(input: Event)
}