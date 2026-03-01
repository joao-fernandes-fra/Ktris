package controller.defaults

import controller.InputHandler
import controller.PieceController
import model.BagRandomizer
import controller.defaults.DefaultTetrisEngine
import model.GameConfig
import model.GameEventBus
import model.GameTimers
import model.Piece
import model.TimeManager

class BaseTetris<T : Piece>(
    settings: GameConfig,
    bagManager: BagRandomizer<T>,
    gameTimers: GameTimers = GameTimers(),
    timeManager: TimeManager = TimeManager(settings),
    gameEventBus: GameEventBus,
    boardManager: BoardManager = BoardManager(settings.boardRows, settings.boardCols),
    pieceController: PieceController<T> = DefaultPieceController(
        boardManager.board,
        settings,
        gameTimers,
        gameEventBus
    ),
) : DefaultTetrisEngine<T>(
    settings, bagManager, gameEventBus, gameTimers, timeManager, boardManager, pieceController, 0f
) {
    private val inputHandler: InputHandler =
        GameInputHandler(this, pieceController, timeManager, bagManager, null, eventBus = gameEventBus)
}