package engine.model.events

import engine.model.GameGoal
import engine.model.GameOverReason
import engine.model.MoveSource
import engine.model.MoveType
import engine.model.Piece
import engine.model.SpinType

object DefaultGameEvents {
    data class GameOver(
        val reason: GameOverReason,
        val goalType: GameGoal,
        override val gameId: String
    ) : GameEvent

    data class PieceMoved(
        val deltaRow: Int,
        val deltaCol: Int,
        val newRow: Int,
        val newCol: Int,
        val moveSource: MoveSource,
        override val gameId: String
    ) : GameEvent

    data class NewPiece(val piece: Piece, override val gameId: String) : GameEvent
    data class PieceHeld(val piece: Piece, override val gameId: String) : GameEvent
    data class PieceRotated(val piece: Piece, val rotationState: Int, override val gameId: String) : GameEvent
    data class HardDrop(var distance: Int, override val gameId: String) : GameEvent
    data class SoftDrop(var distance: Int, override val gameId: String) : GameEvent
    data class PieceLocked(
        val linesCleared: Boolean,
        val piece: Piece,
        val finalRow: Int,
        val finalCol: Int,
        val finalRotationState: Int,
        override val gameId: String
    ) : GameEvent

    data class LineCleared(
        val piece: Piece? = null,
        val spinType: SpinType,
        val linesCleared: Set<Int>,
        val isEmptyBoard: Boolean,
        override val gameId: String
    ) : GameEvent

    data class FreezeLineClear(val linesCleared: Int, val spinType: SpinType, override val gameId: String) : GameEvent
    data class ScoreUpdated(
        val totalLines: Int,
        val pointsEarned: Double,
        val moveType: MoveType,
        override val gameId: String
    ) : GameEvent

    data class SpinDetected(val spinType: SpinType, override val gameId: String) : GameEvent
    data class LevelUp(val newLevel: Int, override val gameId: String) : GameEvent
    data class ComboTriggered(val comboCount: Int, override val gameId: String) : GameEvent
    data class BackToBackTrigger(val backToBackCount: Int, override val gameId: String) : GameEvent
    data class GarbageSent(val lines: Int, val distributionMode: String, override val gameId: String) : GameEvent
    data class GarbageReceived(val lines: Int, override val gameId: String) : GameEvent
}