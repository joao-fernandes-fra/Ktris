package controller

import model.Command
import model.Drop
import model.GameSnapshot
import model.Movement
import model.Piece
import model.Rotation

interface TetrisEngine<T : Piece> {

    val isGameOver: Boolean

    val isGoalMet: Boolean

    var deltaTime: Float

    fun update()

    fun levelUp(): Int

    fun processGarbage(lines: Int, garbageBlockId: Int)

    fun processCommand(command: Command)

    fun onRotation(rotation: Rotation): Boolean

    fun onMovement(movement: Movement): Boolean

    fun processDrop(drop: Drop)

    fun gameStateSnapshot(): GameSnapshot<T>

    fun onRotationRelease(rotation: Rotation)
    fun onMovementRelease(movement: Movement)
}