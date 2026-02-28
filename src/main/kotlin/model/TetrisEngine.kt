package model

interface TetrisEngine {

    val isGameOver: Boolean

    val isVictory: Boolean

    var deltaTime: Float

    fun update()

    fun spawnPiece(nextPiece: Tetromino)

    fun levelUp(): Int

    fun processGarbage(lines: Int, garbageBlockId: Int)

    fun processCommand(command: Command)

    fun processRotation(rotation: Rotation): Boolean

    fun processMove(movement: Movement): Boolean

    fun processDrop(drop: Drop)

    fun gameStateSnapshot(): GameSnapshot

}