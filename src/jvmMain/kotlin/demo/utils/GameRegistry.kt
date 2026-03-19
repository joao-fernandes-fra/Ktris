package demo.utils

import engine.controller.defaults.board.ExtendedBoardController
import engine.controller.defaults.piece.GuidelinePieceController
import engine.controller.defaults.piece.SevenBagRandomizer
import engine.controller.defaults.scoring.DefaultRulebook
import engine.controller.defaults.scoring.ScoreTracker
import engine.controller.defaults.scoring.DefaultScoreEngine
import engine.model.KtrisContext
import engine.model.KtrisContextBuilder
import engine.model.MatchConfig
import engine.model.Piece
import engine.model.PlayerConfig

object GameRegistry {
    private val games = mutableMapOf<String, KtrisContext>()

    fun registerContext(context: KtrisContext): KtrisContext {
        games[context.gameId] = context
        return context
    }

    fun getDefaultContext(
        global: MatchConfig,
        player: PlayerConfig,
        availablePieces: Collection<Piece>,
        gameId: String
    ): KtrisContext {
        val bagRandomizer = SevenBagRandomizer(availablePieces)
        val boardController =
            ExtendedBoardController(global.board.rows, global.board.cols, global.board.bufferZone, global.garbage)
        val pieceController = GuidelinePieceController(
            boardController.board,
            bagRandomizer,
            player,
            global,
            gameId
        )
        val scoringEngine = DefaultScoreEngine(DefaultRulebook())

        return KtrisContextBuilder(gameId)
            .playerSettings(player)
            .gameSettings(global)
            .bagManager(bagRandomizer)
            .boardManager(boardController)
            .pieceController(pieceController)
            .scoringEngine(scoringEngine)
            .scoreTracker(ScoreTracker(gameId))
            .build()
    }

    fun get(gameId: String) =
        games[gameId] ?: error("Game not found: $gameId")
}