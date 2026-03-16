package engine.controller

import engine.model.Board
import engine.model.Drop
import engine.model.GameSnapshot
import engine.model.Movement
import engine.model.Piece
import engine.model.Resetable
import engine.model.Rotation
import engine.model.TimeState

interface GameEngine<T : Piece> : Resetable {
    val gameId: String
    val isGameOver: Boolean
    val isGoalMet: Boolean
    val sessionTimeSeconds: Double
    fun levelUp(newLevel: Int): Int
    fun processGarbage(lines: Int)
    fun onHold(isFreshPress: Boolean = true)
    fun onRotation(rotation: Rotation, isFreshPress: Boolean = true): Boolean
    fun onMovement(movement: Movement): Boolean
    fun onDrop(drop: Drop)
    fun gameStateSnapshot(): GameSnapshot<T>?
    fun onRotationRelease(rotation: Rotation)
    fun onMovementRelease(movement: Movement)
    fun forceBoardState(newState: Board)
    fun onTimeState(timeState: TimeState)
    fun update(deltaTime: Double)
}