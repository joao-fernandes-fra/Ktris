package model.events

import kotlinx.serialization.Serializable
import model.Command
import model.Drop
import model.Movement
import model.Rotation

@Serializable
open class InputEvent : Event {
    @Serializable
    data class SlowDownTime(val duration: Float) : InputEvent() {
        companion object : TopicProvider {
            override val topic = PREFIX + "slow-down"
        }
    }

    @Serializable
    data class FreezeTime(val duration: Float) : InputEvent() {
        companion object : TopicProvider {
            override val topic = PREFIX + "freeze-time"
        }
    }

    @Serializable
    data class DirectionMoveStart(val movement: Movement) : InputEvent() {
        companion object : TopicProvider {
            override val topic = PREFIX + "direction-start"
        }
    }

    @Serializable
    data class DirectionMoveEnd(val movement: Movement) : InputEvent() {
        companion object : TopicProvider {
            override val topic = PREFIX + "direction-end"
        }
    }

    @Serializable
    data class DropInput(val dropType: Drop) : InputEvent() {
        companion object : TopicProvider {
            override val topic = PREFIX + "drop"
        }
    }

    @Serializable
    data class CommandInput(val command: Command) : InputEvent() {
        companion object : TopicProvider {
            override val topic = PREFIX + "command"
        }
    }

    @Serializable
    data class RotationInputStart(val rotation: Rotation) : InputEvent() {
        companion object : TopicProvider {
            override val topic = PREFIX + "rotation-start"
        }
    }

    @Serializable
    data class RotationInputRelease(val rotation: Rotation) : InputEvent() {
        companion object : TopicProvider {
            override val topic = PREFIX + "rotation-release"
        }
    }

    companion object {
        fun registerEvents() {
            EventHandler.register(SlowDownTime::class, SlowDownTime.topic)
            EventHandler.register(FreezeTime::class, FreezeTime.topic)
            EventHandler.register(DirectionMoveStart::class, DirectionMoveStart.topic)
            EventHandler.register(DirectionMoveEnd::class, DirectionMoveEnd.topic)
            EventHandler.register(DropInput::class, DropInput.topic)
            EventHandler.register(CommandInput::class, CommandInput.topic)
            EventHandler.register(RotationInputStart::class, RotationInputStart.topic)
            EventHandler.register(RotationInputRelease::class, RotationInputRelease.topic)
        }

        const val PREFIX = "application.internal.events.input-event."
    }
}
