package engine.controller.defaults

import engine.model.GameSettings
import engine.model.GameTimers
import engine.model.KtrisContext
import engine.model.KtrisContextBuilder
import engine.model.Piece
import engine.model.PlayerSettings
import engine.model.events.GameId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object GameRegistry {
    private val games = mutableMapOf<String, KtrisContext<*>>()

    fun <T : Piece> registerContext(context: KtrisContext<T>): KtrisContext<T> {
        games[context.gameId] = context
        return context
    }

    fun <T : Piece> getDefaultContext(
        global: GameSettings,
        player: PlayerSettings,
        availablePieces: Collection<T>,
        gameId: String
    ): KtrisContext<T> {
        val bagRandomizer = SevenBagRandomizer(availablePieces, player.previewSize)
        val boardController = GuidelineBoardController(global.boardRows, global.boardCols, global.bufferZone)
        val gameTimers = GameTimers()
        val pieceController = GuidelinePieceController(
            boardController.board,
            bagRandomizer,
            player,
            global,
            gameTimers,
            gameId
        )

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + GameId(gameId))

        return KtrisContextBuilder<T>(gameId)
            .playerSettings(player)
            .gameSettings(global)
            .bagManager(bagRandomizer)
            .boardManager(boardController)
            .pieceController(pieceController)
            .gameTimers(gameTimers)
            .scope(scope)
            .build()
    }

    fun <T : Piece> get(gameId: String) = games[gameId] as? KtrisContext<T>
        ?: error("Game not found: $gameId, either not initialized or the Piece type does not match")
}