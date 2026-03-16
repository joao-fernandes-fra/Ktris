package engine.model

import engine.controller.BagRandomizer
import engine.controller.BoardController
import engine.controller.LevelHandler
import engine.controller.PieceController
import engine.controller.defaults.TimeManager
import engine.controller.defaults.scoring.ScoreTracker
import engine.controller.defaults.scoring.ScoringEngine
import kotlinx.coroutines.CoroutineScope

class KtrisContext<T : Piece>(
    val gameId: String,
    val playerSettings: PlayerConfig,
    val gameSettings: MatchConfig,
    val bagManager: BagRandomizer<T>,
    val boardManager: BoardController,
    val pieceController: PieceController<T>,
    val scoringEngine: ScoringEngine,
    val scoreTracker: ScoreTracker?
)

class KtrisContextBuilder<T : Piece>(
    private val gameId: String
) {
    private var playerSettings: PlayerConfig? = null
    private var gameSettings: MatchConfig? = null
    private var bagManager: BagRandomizer<T>? = null
    private var boardManager: BoardController? = null
    private var pieceController: PieceController<T>? = null
    private var scoringEngine: ScoringEngine? = null
    private var scoreTracker: ScoreTracker? = null

    fun playerSettings(settings: PlayerConfig) = apply { this.playerSettings = settings }
    fun gameSettings(settings: MatchConfig) = apply { this.gameSettings = settings }
    fun bagManager(manager: BagRandomizer<T>) = apply { this.bagManager = manager }
    fun boardManager(manager: BoardController) = apply { this.boardManager = manager }
    fun pieceController(controller: PieceController<T>) = apply { this.pieceController = controller }
    fun scoringEngine(engine: ScoringEngine) = apply { this.scoringEngine = engine }
    fun scoreTracker(tracker: ScoreTracker) = apply { this.scoreTracker = tracker }

    fun build(): KtrisContext<T> {
        return KtrisContext(
            gameId = gameId,
            playerSettings = requireNotNull(playerSettings) { "PlayerSettings must be set" },
            gameSettings = requireNotNull(gameSettings) { "GameSettings must be set" },
            bagManager = requireNotNull(bagManager) { "BagManager must be set" },
            boardManager = requireNotNull(boardManager) { "BoardManager must be set" },
            pieceController = requireNotNull(pieceController) { "PieceController must be set" },
            scoringEngine = requireNotNull(scoringEngine) { "ScoringEngine must be set" },
            scoreTracker = scoreTracker
        )
    }
}
