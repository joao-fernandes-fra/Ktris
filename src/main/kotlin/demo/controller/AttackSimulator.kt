package demo.controller

import demo.model.PlayerAPMUpdated
import demo.model.SwingTetris.Companion.ENEMY_GAME_ID
import demo.model.SwingTetris.Companion.PLAYER_GAME_ID
import engine.model.events.EventOrchestrator
import engine.model.events.GameEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AttackSimulator(
    private val scope: CoroutineScope,
    private val apm: Int
) {
    private var receivedGarbageCount = 0
    private var startTime = System.currentTimeMillis()

    fun start() {
        setupListeners()
        scope.launch {
            while (isActive) {
                val lines = (1..4).random()
                EventOrchestrator.publish(
                    GameEvent.GarbageSent(
                        lines = lines,
                        distributionMode = "all",
                        gameId = ENEMY_GAME_ID
                    )
                )

                val baseIntervalPerLine = 60_000.0 / apm
                val intervalMillis = (baseIntervalPerLine * lines).toLong()
                delay(intervalMillis)
            }
        }
        scope.launch {
            while (isActive) {
                val elapsedMinutes = (System.currentTimeMillis() - startTime) / 60_000.0f
                val receivedAPM = if (elapsedMinutes > 0) receivedGarbageCount / elapsedMinutes else 0f
                EventOrchestrator.publish(PlayerAPMUpdated(receivedAPM, PLAYER_GAME_ID))
                delay(16L)
            }
        }
    }

    private fun setupListeners() {
        EventOrchestrator.subscribe<GameEvent.GarbageSent, GameEvent.GarbageSent>(
            { event ->
                if (event != null && event.gameId != ENEMY_GAME_ID) {
                    receivedGarbageCount += event.lines
                }
            },
            { it }
        )
    }
}
