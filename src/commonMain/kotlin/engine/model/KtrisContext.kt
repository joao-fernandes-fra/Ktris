package engine.model

import engine.controller.BagRandomizer
import engine.controller.BoardController
import engine.controller.PieceController
import engine.controller.defaults.TimeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

data class KtrisContext<T : Piece>(
    val gameId: String,
    val playerSettings: PlayerSettings,
    val gameSettings: GameSettings,
    val bagManager: BagRandomizer<T>,
    val gameTimers: GameTimers = GameTimers(),
    val timeManager: TimeManager = TimeManager(gameSettings),
    val boardManager: BoardController,
    val pieceController: PieceController<T>,
    val scope: CoroutineScope?
)

class KtrisContextBuilder<T : Piece>(
    private val gameId: String
) {
    private var playerSettings: PlayerSettings? = null
    private var gameSettings: GameSettings? = null
    private var bagManager: BagRandomizer<T>? = null
    private var gameTimers: GameTimers = GameTimers()
    private var timeManager: TimeManager? = null
    private var boardManager: BoardController? = null
    private var pieceController: PieceController<T>? = null
    private var scope: CoroutineScope? = null

    fun playerSettings(settings: PlayerSettings) = apply { this.playerSettings = settings }
    fun gameSettings(settings: GameSettings) = apply {
        this.gameSettings = settings
        this.timeManager = TimeManager(settings) // auto-init if provided
    }

    fun bagManager(manager: BagRandomizer<T>) = apply { this.bagManager = manager }
    fun gameTimers(timers: GameTimers) = apply { this.gameTimers = timers }
    fun timeManager(manager: TimeManager) = apply { this.timeManager = manager }
    fun boardManager(manager: BoardController) = apply { this.boardManager = manager }
    fun pieceController(controller: PieceController<T>) = apply { this.pieceController = controller }
    fun scope(scope: CoroutineScope) = apply { this.scope = scope }

    fun build(): KtrisContext<T> {
        return KtrisContext(
            gameId = gameId,
            playerSettings = requireNotNull(playerSettings) { "PlayerSettings must be set" },
            gameSettings = requireNotNull(gameSettings) { "GameSettings must be set" },
            bagManager = requireNotNull(bagManager) { "BagManager must be set" },
            gameTimers = gameTimers,
            timeManager = timeManager ?: TimeManager(requireNotNull(gameSettings)),
            boardManager = requireNotNull(boardManager) { "BoardManager must be set" },
            pieceController = requireNotNull(pieceController) { "PieceController must be set" },
            scope = scope
        )
    }
}
