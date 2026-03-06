package demo.controller

import engine.model.Command
import engine.model.Drop
import engine.model.Movement
import engine.model.Rotation
import engine.model.events.EventOrchestrator
import engine.model.events.GameId
import engine.model.events.InputEvent
import kotlinx.coroutines.CoroutineScope
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

class SwingInputHandler(scope: CoroutineScope) : KeyAdapter() {
    private val gameId = scope.coroutineContext[GameId]?.value ?: error("No GameId in scope")
    private var isFrozen = false

    override fun keyPressed(e: KeyEvent?) {
        when (e?.keyCode) {
            KeyEvent.VK_SPACE -> EventOrchestrator.publish(InputEvent.DropInput(Drop.HARD_DROP, gameId))
            KeyEvent.VK_Z -> EventOrchestrator.publish(InputEvent.RotationInputStart(Rotation.ROTATE_CCW, gameId))
            KeyEvent.VK_X -> EventOrchestrator.publish(InputEvent.RotationInputStart(Rotation.ROTATE_CW, gameId))
            KeyEvent.VK_UP -> EventOrchestrator.publish(InputEvent.RotationInputStart(Rotation.ROTATE_180, gameId))
            KeyEvent.VK_C -> EventOrchestrator.publish(InputEvent.CommandInput(Command.HOLD, gameId))
            KeyEvent.VK_DOWN -> EventOrchestrator.publish(InputEvent.DropInput(Drop.SOFT_DROP, gameId))
            KeyEvent.VK_LEFT -> EventOrchestrator.publish(InputEvent.DirectionMoveStart(Movement.MOVE_LEFT, gameId))
            KeyEvent.VK_RIGHT -> EventOrchestrator.publish(InputEvent.DirectionMoveStart(Movement.MOVE_RIGHT, gameId))
            KeyEvent.VK_S -> {
                if (!isFrozen) {
                    EventOrchestrator.publish(InputEvent.FreezeTime(Float.MAX_VALUE, gameId))
                } else {
                    EventOrchestrator.publish(InputEvent.FreezeTime(Float.MIN_VALUE, gameId))
                }
                isFrozen = !isFrozen
            }

            KeyEvent.VK_R -> EventOrchestrator.publish(InputEvent.CommandInput(Command.RESET, gameId))
        }
    }

    override fun keyReleased(e: KeyEvent?) {
        when (e?.keyCode) {
            KeyEvent.VK_LEFT -> EventOrchestrator.publish(InputEvent.DirectionMoveEnd(Movement.MOVE_LEFT, gameId))
            KeyEvent.VK_RIGHT -> EventOrchestrator.publish(InputEvent.DirectionMoveEnd(Movement.MOVE_RIGHT, gameId))
            KeyEvent.VK_Z -> EventOrchestrator.publish(InputEvent.RotationInputRelease(Rotation.ROTATE_CCW, gameId))
            KeyEvent.VK_X -> EventOrchestrator.publish(InputEvent.RotationInputRelease(Rotation.ROTATE_CW, gameId))
        }
    }
}
