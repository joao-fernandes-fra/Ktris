package demo

import controller.defaults.TimeManager
import kotlinx.browser.window
import org.w3c.dom.events.KeyboardEvent
import model.Command
import model.Drop
import model.Movement
import model.Rotation
import model.TimeMode
import model.events.EventHandler
import model.events.InputEvent.*

class WebInputHandler(private val timeManager: TimeManager) {
    init {
        window.addEventListener("keydown", { event ->
            val e = event as KeyboardEvent
            if (e.key in setOf("ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight", " ", "z", "x", "c", "s", "r")) {
                e.preventDefault()
            }
            when (e.key) {
                " " -> EventHandler.publish(DropInput.topic, DropInput(Drop.HARD_DROP))
                "z" -> EventHandler.publish(RotationInputStart.topic, RotationInputStart(Rotation.ROTATE_CCW))
                "x" -> EventHandler.publish(RotationInputStart.topic, RotationInputStart(Rotation.ROTATE_CW))
                "ArrowUp" -> EventHandler.publish(RotationInputStart.topic, RotationInputStart(Rotation.ROTATE_180))
                "c" -> EventHandler.publish(CommandInput.topic, CommandInput(Command.HOLD))
                "ArrowDown" -> EventHandler.publish(DropInput.topic, DropInput(Drop.SOFT_DROP))
                "ArrowLeft" -> EventHandler.publish(DirectionMoveStart.topic, DirectionMoveStart(Movement.MOVE_LEFT))
                "ArrowRight" -> EventHandler.publish(DirectionMoveStart.topic, DirectionMoveStart(Movement.MOVE_RIGHT))
                "s" -> {
                    if (timeManager.mode == TimeMode.FROZEN) {
                        timeManager.resetState()
                    } else {
                        timeManager.freezeTime(Float.MAX_VALUE)
                    }
                }
                "r" -> EventHandler.publish(CommandInput.topic, CommandInput(Command.RESET))
            }
        })

        window.addEventListener("keyup", { event ->
            val e = event as KeyboardEvent
            when (e.key) {
                "ArrowLeft" -> EventHandler.publish(DirectionMoveEnd.topic, DirectionMoveEnd(Movement.MOVE_LEFT))
                "ArrowRight" -> EventHandler.publish(DirectionMoveEnd.topic, DirectionMoveEnd(Movement.MOVE_RIGHT))
                "z" -> EventHandler.publish(RotationInputRelease.topic, RotationInputRelease(Rotation.ROTATE_CCW))
                "x" -> EventHandler.publish(RotationInputRelease.topic, RotationInputRelease(Rotation.ROTATE_CW))
            }
        })
    }
}