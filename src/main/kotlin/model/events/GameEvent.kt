package model.events

import kotlinx.serialization.Serializable
import model.GameGoal
import model.Piece
import model.SpinType

@Serializable
open class GameEvent : Event {
    @Serializable
    data class GameOver(val goalMet: Boolean, val goal: GameGoal) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "game-over" }
    }

    @Serializable
    data class NewPiece(val piece: Piece) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "new-piece" }
    }

    @Serializable
    data class PieceHeld(val piece: Piece) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "piece-held" }
    }

    @Serializable
    data class PieceRotated(val piece: Piece, val rotationState: Int) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "piece-rotated" }
    }

    @Serializable
    data class HardDrop(var distance: Int) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "hard-drop" }
    }

    @Serializable
    data class SoftDrop(var distance: Int) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "soft-drop" }
    }

    @Serializable
    data class PieceLocked(val linesCleared: Boolean) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "piece-locked" }
    }

    @Serializable
    data class LineCleared(val spinType: SpinType, val linesCleared: Int, val isPerfectClear: Boolean) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "line-cleared" }
    }

    @Serializable
    data class FreezeLineClear(val linesCleared: Int, val spinType: SpinType) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "freeze-line-cleared" }
    }

    @Serializable
    data class ScoreUpdated(
        val totalLines: Int,
        val currentPoints: Double,
        val pointsEarned: Double,
        val moveTypeName: String?,
        val comboCount: Int,
        val backToBackCount: Int
    ) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "score-updated" }
    }

    @Serializable
    data class SpinDetected(val spinType: SpinType) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "spin-detected" }
    }

    @Serializable
    data class LevelUp(val newLevel: Int) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "level-up" }
    }

    @Serializable
    data class ComboTriggered(val comboCount: Int) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "combo-triggered" }
    }

    @Serializable
    data class BackToBackTrigger(val backToBackCount: Int) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "back-to-back" }
    }

    @Serializable
    data class SfxTrigger(val soundName: String) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "sfx-trigger" }
    }

    @Serializable
    data class GarbageSent(val lines: Int) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "garbage-sent" }
    }

    @Serializable
    data class GarbageReceived(val lines: Int) : GameEvent() {
        companion object : TopicProvider { @JvmStatic override val topic = PREFIX + "garbage-received" }
    }

    companion object {
        const val PREFIX = "application.internal.events.game-event."
        fun registerEvents() {
            EventHandler.register(GameOver::class, GameOver.topic)
            EventHandler.register(NewPiece::class, NewPiece.topic)
            EventHandler.register(PieceHeld::class, PieceHeld.topic)
            EventHandler.register(PieceRotated::class, PieceRotated.topic)
            EventHandler.register(HardDrop::class, HardDrop.topic)
            EventHandler.register(SoftDrop::class, SoftDrop.topic)
            EventHandler.register(PieceLocked::class, PieceLocked.topic)
            EventHandler.register(LineCleared::class, LineCleared.topic)
            EventHandler.register(FreezeLineClear::class, FreezeLineClear.topic)
            EventHandler.register(ScoreUpdated::class, ScoreUpdated.topic)
            EventHandler.register(SpinDetected::class, SpinDetected.topic)
            EventHandler.register(LevelUp::class, LevelUp.topic)
            EventHandler.register(ComboTriggered::class, ComboTriggered.topic)
            EventHandler.register(BackToBackTrigger::class, BackToBackTrigger.topic)
            EventHandler.register(SfxTrigger::class, SfxTrigger.topic)
            EventHandler.register(GarbageSent::class, GarbageSent.topic)
            EventHandler.register(GarbageReceived::class, GarbageReceived.topic)
        }
    }
}

