package engine.controller

import engine.model.events.GameEvent

interface CommandRecorder {
    fun record(command: GameEvent, timestamp: Float)
}