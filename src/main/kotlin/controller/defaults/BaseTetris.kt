package controller.defaults

import controller.GameRenderer
import controller.InputHandler
import controller.PieceController
import model.BagRandomizer
import model.GameSettings
import model.GameTimers
import model.Piece

class BaseTetris<T : Piece>(
    settings: GameSettings,
    bagManager: BagRandomizer<T>,
    gameTimers: GameTimers = GameTimers(),
    timeManager: TimeManager = TimeManager(settings),
    boardManager: BoardManager = BoardManager(settings.boardRows, settings.boardCols, settings.bufferHeight),
    pieceController: PieceController<T> = DefaultGuidelinePieceController(
        boardManager.board, settings, gameTimers
    ),
) : DefaultTetrisEngine<T>(
    settings, bagManager, gameTimers, timeManager, boardManager, pieceController
) {
    private val inputHandler: InputHandler =
        GameInputHandler(this, pieceController, timeManager, bagManager, null)

    override fun start(renderer: GameRenderer<T>) {
        val targetFps = 60
        val frameTimeNs = 1_000_000_000L / targetFps

        var lastTime = System.nanoTime()

        while (!isGameOver && !isGoalMet) {
            val currentTime = System.nanoTime()
            val elapsedNs = currentTime - lastTime

            val deltaTimeMs = elapsedNs / 1_000_000f

            update(deltaTimeMs)

            lastTime = currentTime

            renderer.render(gameStateSnapshot())

            val workTimeNs = System.nanoTime() - currentTime
            val sleepTimeNs = frameTimeNs - workTimeNs

            if (sleepTimeNs > 0) {
                Thread.sleep(sleepTimeNs / 1_000_000L, (sleepTimeNs % 1_000_000L).toInt())
            }
        }
    }
}