package engine.controller.defaults

import engine.controller.GameRenderer
import engine.model.Piece
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

open class BaseTetris<T : Piece>(
    scope: CoroutineScope,
) : DefaultTetrisEngine<T>(
    scope
) {
    override suspend fun start(renderer: GameRenderer<T>) {
        scope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                val now = System.currentTimeMillis()
                val deltaMillis = now - lastTime
                lastTime = now

                update(deltaMillis.toFloat())
                renderer.render(gameStateSnapshot())

                delay(1)
            }
        }
    }
}