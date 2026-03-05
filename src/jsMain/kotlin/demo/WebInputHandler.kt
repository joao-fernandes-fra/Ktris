package demo

import controller.defaults.ScoreRegistry
import kotlinx.browser.window
import model.Command
import model.Drop
import model.Movement
import model.Rotation
import model.events.EventHandler
import model.events.InputEvent
import model.events.InputEvent.CommandInput
import model.events.InputEvent.DirectionMoveEnd
import model.events.InputEvent.DirectionMoveStart
import model.events.InputEvent.DropInput
import model.events.InputEvent.RotationInputRelease
import model.events.InputEvent.RotationInputStart
import org.w3c.dom.events.KeyboardEvent

class WebInputHandler(private val scoreRegistry: ScoreRegistry) {
    private var isTimeFrozen = false

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
                    if (!isTimeFrozen) {
                        EventHandler.publish(InputEvent.FreezeTime.topic, InputEvent.FreezeTime(Float.MAX_VALUE))
                    } else {
                        EventHandler.publish(InputEvent.FreezeTime.topic, InputEvent.FreezeTime(Float.MIN_VALUE))
                    }
                    isTimeFrozen = !isTimeFrozen
                }

                "r" -> {
                    scoreRegistry.reset()
                    EventHandler.publish(CommandInput.topic, CommandInput(Command.RESET))
                }
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