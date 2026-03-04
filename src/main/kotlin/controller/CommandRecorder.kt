package controller

import model.events.InputEvent

interface CommandRecorder {
    fun record(command: InputEvent, timestamp: Float)
}