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
    suspend fun start(renderer: GameRenderer<T>)
    suspend fun levelUp(newLevel: Int): Int
    suspend fun processGarbage(lines: Int, garbageBlockId: Int)
    suspend fun onCommand(command: Command)
    suspend fun onRotation(rotation: Rotation): Boolean
    suspend fun onMovement(movement: Movement): Boolean
    suspend fun onDrop(drop: Drop)
    suspend fun gameStateSnapshot(): GameSnapshot<T>
    suspend fun onRotationRelease(rotation: Rotation)
    suspend fun onMovementRelease(movement: Movement)
    suspend fun forceBoardState(newState: Board)
    suspend fun onTimeState(timeState: TimeState, duration: Double)
}