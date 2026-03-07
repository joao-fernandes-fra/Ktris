package engine.controller.defaults

import engine.controller.GameRenderer
import engine.model.KtrisContext
import engine.model.Piece
import engine.model.events.GameId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.DurationUnit

open class BaseTetris<T : Piece>(
    context: KtrisContext<T>
) : DefaultTetrisEngine<T>(
    context.playerSettings,
    context.gameSettings,
    context.boardManager,
    context.pieceController,
    context.gameTimers,
    context.timeManager,
) {
    private var scope: CoroutineScope
    override val gameId: String = context.gameId

    init {
        scope = context.scope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default + GameId(gameId))
    }

    override suspend fun start(renderer: GameRenderer<T>) {
        scope.launch {
            var lastTime = Clock.System.now()
            while (isActive) {
                val now = Clock.System.now()
                val deltaMillis = now - lastTime
                lastTime = now

                update(deltaMillis.toDouble(DurationUnit.MILLISECONDS))
                renderer.render(gameStateSnapshot())

                delay(1)
            }
        }
    }
}