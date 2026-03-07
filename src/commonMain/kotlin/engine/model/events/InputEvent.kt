package engine.model.events

import engine.model.Command
import engine.model.Drop
import engine.model.Movement
import engine.model.Rotation

object InputEvent {
    data class SlowDownTime(val duration: Double, override val gameId: String) : Event
    data class FreezeTime(val duration: Double, override val gameId: String) : Event
    data class DirectionMoveStart(val movement: Movement, override val gameId: String) : Event
    data class DirectionMoveEnd(val movement: Movement, override val gameId: String) : Event
    data class DropInput(val dropType: Drop, override val gameId: String) : Event
    data class CommandInput(val command: Command, override val gameId: String) : Event
    data class RotationInputStart(val rotation: Rotation, override val gameId: String) : Event
    data class RotationInputRelease(val rotation: Rotation, override val gameId: String) : Event
}
