package engine.model.events

import engine.model.GameGoal
import engine.model.MoveType
import engine.model.Piece
import engine.model.SpinType

object GameEvent {
    data class GameOver(val goalMet: Boolean, val goal: GameGoal, override val gameId: String) : Event
    data class NewPiece(val piece: Piece, override val gameId: String) : Event
    data class PieceHeld(val piece: Piece, override val gameId: String) : Event
    data class PieceRotated(val piece: Piece, val rotationState: Int, override val gameId: String) : Event
    data class HardDrop(var distance: Int, override val gameId: String) : Event
    data class SoftDrop(var distance: Int, override val gameId: String) : Event
    data class PieceLocked(val linesCleared: Boolean, override val gameId: String) : Event
    data class LineCleared(val spinType: SpinType, val linesCleared: Set<Int>, val isEmptyBoard: Boolean, override val gameId: String) : Event
    data class FreezeLineClear(val linesCleared: Int, val spinType: SpinType, override val gameId: String) : Event
    data class ScoreUpdated(val totalLines: Int, val currentPoints: Double, val pointsEarned: Double, val moveType: MoveType, override val gameId: String) : Event
    data class SpinDetected(val spinType: SpinType, override val gameId: String) : Event
    data class LevelUp(val newLevel: Int, override val gameId: String) : Event
    data class ComboTriggered(val comboCount: Int, override val gameId: String) : Event
    data class BackToBackTrigger(val backToBackCount: Int, override val gameId: String) : Event
    data class GarbageSent(val lines: Int, val distributionMode: String, override val gameId: String) : Event
    data class GarbageReceived(val lines: Int, override val gameId: String) : Event
}