package controller.defaults

import controller.CommandRecorder
import controller.InputHandler
import model.Piece
import model.events.EventHandler
import model.events.InputEvent
import model.events.InputEvent.CommandInput
import model.events.InputEvent.DirectionMoveEnd
import model.events.InputEvent.DirectionMoveStart
import model.events.InputEvent.DropInput
import model.events.InputEvent.FreezeTime
import model.events.InputEvent.RotationInputRelease
import model.events.InputEvent.RotationInputStart
import model.events.InputEvent.SlowDownTime
import platform.currentTimeMillis

class GameInputHandler<T : Piece>(
    private val engine: DefaultTetrisEngine<T>,
    private val timeManager: TimeManager,
    private val commandRecorder: CommandRecorder?,
) : InputHandler {
    init {
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        EventHandler.subscribeToEvent<DirectionMoveStart> { handleInput(it) }
        EventHandler.subscribeToEvent<DirectionMoveEnd> { handleInput(it) }
        EventHandler.subscribeToEvent<DropInput> { handleInput(it) }
        EventHandler.subscribeToEvent<CommandInput> { handleInput(it) }
        EventHandler.subscribeToEvent<RotationInputStart> { handleInput(it) }
        EventHandler.subscribeToEvent<RotationInputRelease> { handleInput(it) }
        EventHandler.subscribeToEvent<SlowDownTime> { handleInput(it) }
        EventHandler.subscribeToEvent<FreezeTime> { handleInput(it) }
    }

    override fun handleInput(input: InputEvent) {
        if (engine.isGameOver || engine.isGoalMet) return

        when (input) {
            is DirectionMoveStart -> engine.onMovement(input.movement)
            is DirectionMoveEnd -> engine.onMovementRelease(input.movement)
            is DropInput -> engine.processDrop(input.dropType)
            is CommandInput -> engine.processCommand(input.command)
            is RotationInputStart -> engine.onRotation(input.rotation)
            is RotationInputRelease -> engine.onRotationRelease(input.rotation)
            is SlowDownTime -> timeManager.slowDownTime(input.duration)
            is FreezeTime -> timeManager.freezeTime(input.duration)
        }
        commandRecorder?.record(input, currentTimeMillis().toFloat() / 1000f)
    }
}