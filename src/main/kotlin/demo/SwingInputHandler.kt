package demo

import controller.defaults.TimeManager
import model.Command
import model.Drop
import model.Movement
import model.Rotation
import model.TimeMode
import model.events.EventHandler
import model.events.InputEvent.CommandInput
import model.events.InputEvent.DirectionMoveEnd
import model.events.InputEvent.DirectionMoveStart
import model.events.InputEvent.DropInput
import model.events.InputEvent.RotationInputRelease
import model.events.InputEvent.RotationInputStart
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

class SwingInputHandler(
    private val timeManager: TimeManager
) : KeyAdapter() {
    override fun keyPressed(e: KeyEvent?) {
        when (e?.keyCode) {
            KeyEvent.VK_SPACE ->  EventHandler.publish(DropInput.topic, DropInput(Drop.HARD_DROP))
            KeyEvent.VK_Z ->  EventHandler.publish(RotationInputStart.topic, RotationInputStart(Rotation.ROTATE_CCW))
            KeyEvent.VK_X ->  EventHandler.publish(RotationInputStart.topic, RotationInputStart(Rotation.ROTATE_CW))
            KeyEvent.VK_UP ->  EventHandler.publish(RotationInputStart.topic, RotationInputStart(Rotation.ROTATE_180))
            KeyEvent.VK_C ->  EventHandler.publish(CommandInput.topic, CommandInput(Command.HOLD))
            KeyEvent.VK_DOWN ->  EventHandler.publish(DropInput.topic,DropInput(Drop.SOFT_DROP))
            KeyEvent.VK_LEFT ->  EventHandler.publish(DirectionMoveStart.topic, DirectionMoveStart(Movement.MOVE_LEFT))
            KeyEvent.VK_RIGHT -> EventHandler.publish(DirectionMoveStart.topic, DirectionMoveStart(Movement.MOVE_RIGHT))
            KeyEvent.VK_S -> {
                if (timeManager.mode == TimeMode.FROZEN) {
                    timeManager.resetState()
                } else {
                    timeManager.freezeTime(Float.MAX_VALUE)
                }
            }
        }
    }

    override fun keyReleased(e: KeyEvent?) {
        when (e?.keyCode) {
            KeyEvent.VK_LEFT -> EventHandler.publish(DirectionMoveEnd.topic, DirectionMoveEnd(Movement.MOVE_LEFT))
            KeyEvent.VK_RIGHT -> EventHandler.publish(DirectionMoveEnd.topic, DirectionMoveEnd(Movement.MOVE_RIGHT))
            KeyEvent.VK_Z -> EventHandler.publish(RotationInputRelease.topic, RotationInputRelease(Rotation.ROTATE_CCW))
            KeyEvent.VK_X -> EventHandler.publish(RotationInputRelease.topic, RotationInputRelease(Rotation.ROTATE_CW))
        }
    }
}