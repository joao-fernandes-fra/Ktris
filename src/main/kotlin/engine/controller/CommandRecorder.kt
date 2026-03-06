package engine.controller

import engine.model.events.Event

interface CommandRecorder {
    fun record(command: Event, timestamp: Float)
}