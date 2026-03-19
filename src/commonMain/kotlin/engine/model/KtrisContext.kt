package engine.model

import engine.controller.PieceRandomizer
import engine.controller.BoardController
import engine.controller.PieceController
import engine.controller.ScoreEngine
import engine.controller.defaults.scoring.ScoreTracker
import engine.controller.defaults.scoring.DefaultScoreEngine

class KtrisContext(
    val gameId: String,
    val playerSettings: PlayerConfig,
    val gameSettings: MatchConfig,
    val bagManager: PieceRandomizer,
    val boardManager: BoardController,
    val pieceController: PieceController,
    val scoringEngine: ScoreEngine,
    val scoreTracker: ScoreTracker?
)

class KtrisContextBuilder(
    private val gameId: String
) {
    private var playerSettings: PlayerConfig? = null
    private var gameSettings: MatchConfig? = null
    private var bagManager: PieceRandomizer? = null
    private var boardManager: BoardController? = null
    private var pieceController: PieceController? = null
    private var scoringEngine: ScoreEngine? = null
    private var scoreTracker: ScoreTracker? = null

    fun playerSettings(settings: PlayerConfig) = apply { this.playerSettings = settings }
    fun gameSettings(settings: MatchConfig) = apply { this.gameSettings = settings }
    fun bagManager(manager: PieceRandomizer) = apply { this.bagManager = manager }
    fun boardManager(manager: BoardController) = apply { this.boardManager = manager }
    fun pieceController(controller: PieceController) = apply { this.pieceController = controller }
    fun scoringEngine(engine: DefaultScoreEngine) = apply { this.scoringEngine = engine }
    fun scoreTracker(tracker: ScoreTracker) = apply { this.scoreTracker = tracker }

    fun build(): KtrisContext {
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
