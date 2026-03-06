package engine.controller.defaults

import engine.controller.GameRenderer
import engine.model.Piece
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.DurationUnit

open class BaseTetris<T : Piece>(
    scope: CoroutineScope,
) : DefaultTetrisEngine<T>(
    scope
) {
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