package engine.model

import engine.controller.BagRandomizer
import engine.controller.BoardController
import engine.controller.PieceController
import engine.controller.defaults.TimeManager
import kotlinx.coroutines.CoroutineScope

data class KtrisContext<T : Piece>(
    val gameId: String,
    val playerSettings: PlayerSettings,
    val globalSettings: GlobalGameSettings,
    val bagManager: BagRandomizer<T>,
    val gameTimers: GameTimers = GameTimers(),
    val timeManager: TimeManager,
    val boardManager: BoardController,
    val pieceController: PieceController<T>,
    val scope: CoroutineScope
)