package handson

import model.Command
import model.Drop
import model.GameEventBus
import model.InputEvent
import model.Movement
import model.Rotation
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

class SwingInputHandler(private val eventBus: GameEventBus) : KeyAdapter() {
    override fun keyPressed(e: KeyEvent?) {
        when (e?.keyCode) {
            KeyEvent.VK_SPACE -> postEvent(InputEvent.DropInput(Drop.HARD_DROP))
            KeyEvent.VK_Z -> postEvent(InputEvent.RotationInput(Rotation.ROTATE_CCW))
            KeyEvent.VK_X -> postEvent(InputEvent.RotationInput(Rotation.ROTATE_CW))
            KeyEvent.VK_UP -> postEvent(InputEvent.RotationInput(Rotation.ROTATE_180))
            KeyEvent.VK_C -> postEvent(InputEvent.CommandInput(Command.HOLD))
            KeyEvent.VK_DOWN -> postEvent(InputEvent.DropInput(Drop.SOFT_DROP))
            KeyEvent.VK_LEFT -> postEvent(InputEvent.DirectionMoveStart(Movement.MOVE_LEFT))
            KeyEvent.VK_RIGHT -> postEvent(InputEvent.DirectionMoveStart(Movement.MOVE_RIGHT))
        }
    }

    override fun keyReleased(e: KeyEvent?) {
        when (e?.keyCode) {
            KeyEvent.VK_LEFT -> postEvent(InputEvent.DirectionMoveEnd(Movement.MOVE_LEFT))
            KeyEvent.VK_RIGHT -> postEvent(InputEvent.DirectionMoveEnd(Movement.MOVE_RIGHT))
        }
    }

    private fun postEvent(event: InputEvent) {
        eventBus.post(event)
    }
}