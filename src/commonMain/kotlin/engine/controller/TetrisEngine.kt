package engine.controller

import engine.model.Board
import engine.model.Command
import engine.model.Drop
import engine.model.GameSnapshot
import engine.model.Movement
import engine.model.Piece
import engine.model.Resetable
import engine.model.Rotation
import engine.model.TimeState

interface TetrisEngine<T : Piece> : Resetable {
    val gameId: String
    val isGameOver: Boolean
    val isGoalMet: Boolean
    val sessionTimeSeconds: Double
    fun start(renderer: GameRenderer<T>)
    fun levelUp(newLevel: Int): Int
    fun processGarbage(lines: Int)
    fun onHold()
    fun onRotation(rotation: Rotation): Boolean
    fun onMovement(movement: Movement): Boolean
    fun onDrop(drop: Drop)
    fun gameStateSnapshot(): GameSnapshot<T>?
    fun onRotationRelease(rotation: Rotation)
    fun onMovementRelease(movement: Movement)
    fun forceBoardState(newState: Board)
    fun onTimeState(timeState: TimeState, duration: Double)
}