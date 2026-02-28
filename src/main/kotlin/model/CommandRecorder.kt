package model

interface CommandRecorder {
    fun record(command: InputEvent, timestamp: Float)
}