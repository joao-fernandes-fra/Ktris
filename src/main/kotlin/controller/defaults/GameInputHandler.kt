package controller.defaults

import controller.CommandRecorder
import controller.InputHandler
import controller.PieceController
import model.BagRandomizer
import model.GameEventBus
import model.InputEvent
import model.Piece

class GameInputHandler<T : Piece>(
    private val engine: DefaultTetrisEngine<T>,
    private val pieceController: PieceController<T>,
    private val timeManager: TimeManager,
    private val bagRandomizer: BagRandomizer<T>,
    private val commandRecorder: CommandRecorder?,
    eventBus: GameEventBus
) : InputHandler {

    init {
        eventBus.subscribe<InputEvent> { event ->
            handleInput(event)
        }
    }

    override fun handleInput(input: InputEvent) {
        if (engine.isGameOver || engine.isGoalMet) return

        when (input) {
            is InputEvent.DirectionMoveStart -> engine.onMovement(input.movement)
            is InputEvent.DirectionMoveEnd -> engine.onMovementRelease(input.movement)
            is InputEvent.DropInput -> engine.processDrop(input.dropType)
            is InputEvent.CommandInput -> pieceController.holdPiece { bagRandomizer.getNextPiece() }
            is InputEvent.RotationInputStart -> engine.onRotation(input.rotation)
            is InputEvent.RotationInputRelease -> engine.onRotationRelease(input.rotation)
            is InputEvent.SlowDownTime -> timeManager.slowDownTime(input.duration)
            is InputEvent.FreezeTime -> timeManager.freezeTime(input.duration)
        }
        commandRecorder?.record(input, System.currentTimeMillis().toFloat() / 1000f)
    }
}