package engine.controller.defaults

import engine.model.KtrisContext
import engine.model.KtrisContextBuilder
import engine.model.MatchConfig
import engine.model.Piece
import engine.model.PlayerConfig

object GameRegistry {
    private val games = mutableMapOf<String, KtrisContext<*>>()

    fun <T : Piece> registerContext(context: KtrisContext<T>): KtrisContext<T> {
        games[context.gameId] = context
        return context
    }

    fun <T : Piece> getDefaultContext(
        global: MatchConfig,
        player: PlayerConfig,
        availablePieces: Collection<T>,
        gameId: String
    ): KtrisContext<T> {
        val bagRandomizer = SevenBagRandomizer(availablePieces, global.gameplay.previewSize)
        val boardController = GuidelineBoardController(global.board.rows, global.board.cols, global.board.bufferZone)
        val pieceController = GuidelinePieceController(
            boardController.board,
            bagRandomizer,
            player,
            global,
            gameId
        )


        return KtrisContextBuilder<T>(gameId)
            .playerSettings(player)
            .gameSettings(global)
            .bagManager(bagRandomizer)
            .boardManager(boardController)
            .pieceController(pieceController)
            .build()
    }

    fun <T : Piece> get(gameId: String) =
        games[gameId] ?: error("Game not found: $gameId")
}