package demo.controller

import engine.controller.defaults.DefaultGameEngine
import engine.model.Drop
import engine.model.Movement
import engine.model.Rotation
import engine.model.TimeState
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

class SwingInputHandler(private val engine: DefaultGameEngine) : KeyAdapter() {
    private var isFrozen = false

    override fun keyPressed(e: KeyEvent?) {
        when (e?.keyCode) {
            KeyEvent.VK_SPACE -> engine.onDrop(Drop.HARD_DROP)
            KeyEvent.VK_Z -> engine.onRotation(Rotation.ROTATE_CCW)
            KeyEvent.VK_X -> engine.onRotation(Rotation.ROTATE_CW)
            KeyEvent.VK_UP -> engine.onRotation(Rotation.ROTATE_180)
            KeyEvent.VK_C -> engine.onHold()
            KeyEvent.VK_DOWN -> engine.onDrop(Drop.SOFT_DROP)
            KeyEvent.VK_LEFT -> engine.onMovement(Movement.MOVE_LEFT)
            KeyEvent.VK_RIGHT -> engine.onMovement(Movement.MOVE_RIGHT)
            KeyEvent.VK_S -> if (isFrozen) engine.onTimeState(TimeState.Normal) else engine.onTimeState(TimeState.Frozen())
            KeyEvent.VK_R -> engine.reset()
        }
    }

    override fun keyReleased(e: KeyEvent?) {
        when (e?.keyCode) {
            KeyEvent.VK_LEFT -> engine.onMovementRelease(Movement.MOVE_LEFT)
            KeyEvent.VK_RIGHT -> engine.onMovementRelease(Movement.MOVE_RIGHT)
            KeyEvent.VK_Z -> engine.onRotationRelease(Rotation.ROTATE_CCW)
            KeyEvent.VK_X -> engine.onRotationRelease(Rotation.ROTATE_CW)
            KeyEvent.VK_UP -> engine.onRotationRelease(Rotation.ROTATE_180)
        }
    }
}
