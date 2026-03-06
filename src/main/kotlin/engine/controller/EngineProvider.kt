package engine.controller

import engine.controller.defaults.TimeManager
import engine.model.BagRandomizer
import engine.model.GameTimers
import engine.model.GlobalGameSettings
import engine.model.Piece
import engine.model.PlayerSettings

interface EngineProvider<T : Piece> {
    fun provideSettings(gameId: String): GlobalGameSettings
    fun providePlayerSettings(gameId: String): PlayerSettings
    fun provideBag(gameId: String): BagRandomizer<T>
    fun provideTimers(gameId: String): GameTimers
    fun provideTimeManager(gameId: String): TimeManager
    fun provideBoard(gameId: String): BoardController
    fun providePieceController(gameId: String): PieceController<T>
}

object EngineProviders {
    private val providers = mutableMapOf<String, EngineProvider<*>>()

    fun <T : Piece> register(gameId: String, provider: EngineProvider<T>) {
        providers[gameId] = provider
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Piece> resolve(gameId: String): EngineProvider<T> {
        return providers[gameId] as? EngineProvider<T>
            ?: error("No provider registered for gameId=$gameId")
    }
}
