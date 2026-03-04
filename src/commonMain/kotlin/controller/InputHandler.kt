package controller

import model.events.InputEvent

interface InputHandler {
    fun handleInput(input: InputEvent)
}