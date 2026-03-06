package engine.model.events

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

data class GameId(val value: String) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<GameId>
    override val key: CoroutineContext.Key<*> get() = Key
}

suspend fun currentGameId(): String =
    coroutineContext[GameId]?.value ?: error("No gameId in context")
