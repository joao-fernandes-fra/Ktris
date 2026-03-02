package controller.defaults

import controller.InputHandler
import controller.PieceController
import model.BagRandomizer
import model.GameConfig
import model.GameEventBus
import model.GameTimers
import model.Piece

class BaseTetris<T : Piece>(
    settings: GameConfig,
    bagManager: BagRandomizer<T>,
    gameTimers: GameTimers = GameTimers(),
    timeManager: TimeManager = TimeManager(settings),
    eventBus: GameEventBus,
    boardManager: BoardManager = BoardManager(settings.boardRows, settings.boardCols),
    pieceController: PieceController<T> = DefaultPieceController(
        boardManager.board,
        settings,
        gameTimers,
        eventBus
    ),
) : DefaultTetrisEngine<T>(
    settings, bagManager, eventBus, gameTimers, timeManager, boardManager, pieceController, 0f
) {
    private val inputHandler: InputHandler =
        GameInputHandler(this, pieceController, timeManager, bagManager, null, eventBus = eventBus)
}