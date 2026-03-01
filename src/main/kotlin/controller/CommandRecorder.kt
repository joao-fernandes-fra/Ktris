package controller

import model.InputEvent

interface CommandRecorder {
    fun record(command: InputEvent, timestamp: Float)
}