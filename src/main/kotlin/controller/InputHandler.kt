package controller

import model.InputEvent

interface InputHandler {
    fun handleInput(input: InputEvent)
}