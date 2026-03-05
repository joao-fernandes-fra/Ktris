package controller

import model.Board
import model.Command
import model.Drop
import model.GameSnapshot
import model.Movement
import model.Piece
import model.Resetable
import model.Rotation

interface TetrisEngine<T : Piece> : Resetable {
    val isGameOver: Boolean
    val isGoalMet: Boolean
    fun start(renderer: GameRenderer<T>)
    fun levelUp(): Int
    fun processGarbage(lines: Int, garbageBlockId: Int)
    fun processCommand(command: Command)
    fun onRotation(rotation: Rotation): Boolean
    fun onMovement(movement: Movement): Boolean
    fun processDrop(drop: Drop)
    fun gameStateSnapshot(): GameSnapshot<T>
    fun onRotationRelease(rotation: Rotation)
    fun onMovementRelease(movement: Movement)
    fun forceBoardState(newState: Board)
}