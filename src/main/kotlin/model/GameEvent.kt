package model

import util.TSpinType


sealed class GameEvent {
    data class GameOver(val victory: Boolean, val goal: GameGoal) : GameEvent()
    data class NewPiece(val piece: Tetromino) : GameEvent()
    data class PieceHeld(val piece: Tetromino) : GameEvent()
    data class PieceRotated(val piece: Tetromino, val rotationState: Int) : GameEvent()
    data class HardDrop(var distance: Int) : GameEvent()
    data class SoftDrop(var distance: Int) : GameEvent()
    data class PieceLocked( val linesCleared: Boolean) : GameEvent()
    data class LineCleared(val spinType: TSpinType, val linesCleared: Int, val isPerfectClear: Boolean) : GameEvent()
    data class ScoreUpdated(val totalLines: Int, val currentPoints: Double, val pointsEarned: Double, val moveType: MoveType) : GameEvent()
    data class TSpinDetected(val type: TSpinType) : GameEvent()
    data class LevelUp(val newLevel: Int) : GameEvent()
    data class ComboTriggered(val comboCount: Int) : GameEvent()
    data class BackToBackTrigger(val backToBackCount: Int) : GameEvent()
    data class PauseToggled(val isPaused: Boolean)
    data class SfxTrigger(val soundName: String) : GameEvent()
    data class GarbageSent(val lines: Int) : GameEvent()
    data class GarbageReceived(val lines: Int) : GameEvent()
}

