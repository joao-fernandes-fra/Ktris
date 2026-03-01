package model

import controller.MoveType


sealed class GameEvent {
    data class GameOver(val victory: Boolean, val goal: GameGoal) : GameEvent()
    data class NewPiece(val piece: Piece) : GameEvent()
    data class PieceHeld(val piece: Piece) : GameEvent()
    data class PieceRotated(val piece: Piece, val rotationState: Int) : GameEvent()
    data class HardDrop(var distance: Int) : GameEvent()
    data class SoftDrop(var distance: Int) : GameEvent()
    data class PieceLocked( val linesCleared: Boolean) : GameEvent()
    data class LineCleared(val spinType: SpinType, val linesCleared: Int, val isPerfectClear: Boolean) : GameEvent()
    data class FreezeLineClear(val linesCleared: Int, val spinType: SpinType, ) : GameEvent()
    data class ScoreUpdated(val totalLines: Int, val currentPoints: Double, val pointsEarned: Double, val moveType: MoveType) : GameEvent()
    data class SpinDetected(val type: SpinType) : GameEvent()
    data class LevelUp(val newLevel: Int) : GameEvent()
    data class ComboTriggered(val comboCount: Int) : GameEvent()
    data class BackToBackTrigger(val backToBackCount: Int) : GameEvent()
    data class PauseToggled(val isPaused: Boolean)
    data class SfxTrigger(val soundName: String) : GameEvent()
    data class GarbageSent(val lines: Int) : GameEvent()
    data class GarbageReceived(val lines: Int) : GameEvent()
}

