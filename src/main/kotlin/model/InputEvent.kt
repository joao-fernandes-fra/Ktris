package model

sealed class InputEvent {
    data class DirectionMoveStart(val movement: Movement) : InputEvent()
    data class DirectionMoveEnd(val movement: Movement) : InputEvent()
    data class DropInput(val dropType: Drop) : InputEvent()
    data class CommandInput(val command: Command) : InputEvent()
    data class RotationInputStart(val rotation: Rotation) : InputEvent()
    data class RotationInputRelease(val rotation: Rotation) : InputEvent()
}
