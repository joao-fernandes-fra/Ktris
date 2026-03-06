package engine.controller.defaults

import engine.controller.EngineProvider
import engine.controller.EngineProviders
import engine.model.GameTimers
import engine.model.GlobalGameSettings
import engine.model.KtrisContext
import engine.model.Piece
import engine.model.PlayerSettings
import engine.model.events.GameId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object GameRegistry {
    private val games = mutableMapOf<String, KtrisContext<*>>()

    fun <T : Piece> createGame(context: KtrisContext<T>): KtrisContext<T> {
        EngineProviders.register(context.gameId, object : EngineProvider<T> {
            override fun provideSettings(gameId: String) = context.globalSettings
            override fun providePlayerSettings(gameId: String): PlayerSettings = context.playerSettings
            override fun provideBag(gameId: String) = context.bagManager
            override fun provideTimers(gameId: String) = context.gameTimers
            override fun provideTimeManager(gameId: String) = context.timeManager
            override fun provideBoard(gameId: String) = context.boardManager
            override fun providePieceController(gameId: String) = context.pieceController
        })
        games[context.gameId] = context
        return context
    }

    fun <T : Piece> buildContext(
        global: GlobalGameSettings,
        player: PlayerSettings,
        availablePieces: Collection<T>,
        gameId: String
    ): KtrisContext<T> {
        val boardController = BoardManager(global.boardRows, global.boardCols, global.bufferZone)
        val gameTimers = GameTimers()
        val pieceController = DefaultPieceProvider<T>(boardController.board, player, global, gameTimers, gameId)

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + GameId(gameId))
        val context = KtrisContext(
            gameId = gameId,
            playerSettings = player,
            globalSettings = global,
            gameTimers = gameTimers,
            bagManager = SevenBagRandomizer(availablePieces, player.previewSize),
            timeManager = TimeManager(global),
            boardManager = boardController,
            pieceController = pieceController,
            scope = scope
        )
        return context
    }

    fun <T : Piece> get(gameId: String) = games[gameId] as? KtrisContext<T> ?: error("Game not found: $gameId, either not initialized or the Piece type does not match")
}