package demo.controller

import demo.model.PendingGarbage
import engine.model.events.EventOrchestrator
import engine.model.events.GameEvent
import engine.model.events.GameId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val GARBAGE_ENTRANCE_DELAY = 1000L

class GarbageProcessor(
    scope: CoroutineScope,
    private val delayMillis: Long = GARBAGE_ENTRANCE_DELAY
) {
    private data class GarbagePacket(val lines: Int, val scheduledAt: Long)

    private val garbageQueue = ArrayDeque<GarbagePacket>()
    private val gameId = scope.coroutineContext[GameId]?.value ?: error("No GameId in scope")

    init {
        scope.launch {
            while (isActive) {
                updatePendingGarbage()
                if (garbageQueue.isNotEmpty()) {
                    val packet = garbageQueue.removeFirst()
                    val wait = packet.scheduledAt - System.currentTimeMillis()
                    if (wait > 0) delay(wait)
                    applyGarbage(packet)
                } else {
                    delay(GARBAGE_ENTRANCE_DELAY)
                }
            }
        }
        setupSubscribers()
    }


    private fun setupSubscribers() {
        EventOrchestrator.subscribe<GameEvent.GarbageSent, Int>(
            { lines -> lines?.let { receiveGarbage(it) } },
            { event -> if (event.gameId != gameId) event.lines else null }
        )
    }

    fun receiveGarbage(lines: Int) {
        val packet = GarbagePacket(lines, System.currentTimeMillis() + delayMillis)
        garbageQueue.add(packet)
        updatePendingGarbage()
    }

    fun sendGarbage(lines: Int, distributionMode: String) {
        var remaining = lines

        while (remaining > 0 && garbageQueue.isNotEmpty()) {
            val front = garbageQueue.first()
            if (front.lines <= remaining) {
                remaining -= front.lines
                garbageQueue.removeFirst()
            } else {
                garbageQueue[0] = front.copy(lines = front.lines - remaining)
                remaining = 0
            }
        }

        if (remaining > 0) {
            EventOrchestrator.publish(GameEvent.GarbageSent(remaining, distributionMode, gameId))
        }
    }

    private fun applyGarbage(packet: GarbagePacket) {
        EventOrchestrator.publish(GameEvent.GarbageReceived(packet.lines, gameId))
    }

    private fun updatePendingGarbage() {
        val pendingLines = garbageQueue.sumOf { it.lines }
        EventOrchestrator.publish(PendingGarbage(pendingLines, gameId))
    }
}
