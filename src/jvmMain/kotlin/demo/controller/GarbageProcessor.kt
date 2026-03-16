package demo.controller

import demo.model.PendingGarbage
import engine.model.events.DefaultGameEvents
import engine.model.events.EventOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val GARBAGE_ENTRANCE_DELAY = 1000L

class GarbageProcessor(
    private val scope: CoroutineScope,
    private val delayMillis: Long = GARBAGE_ENTRANCE_DELAY,
    private val gameId: String
) {
    private data class GarbagePacket(val lines: Int, val scheduledAt: Long)

    private val queueMutex = Mutex()
    private val garbageQueue = ArrayDeque<GarbagePacket>()
    private var isGameOver: Boolean = false

    init {
        startProcessing()
        setupSubscribers()
    }

    private fun startProcessing() {
        scope.launch {
            while (isActive && !isGameOver) {
                val packet = queueMutex.withLock {
                    if (garbageQueue.isNotEmpty()) garbageQueue.removeFirst() else null
                }

                if (packet != null) {
                    val wait = packet.scheduledAt - System.currentTimeMillis()
                    if (wait > 0) delay(wait)
                    applyGarbage(packet)
                    updatePendingGarbage()
                } else {
                    delay(GARBAGE_ENTRANCE_DELAY)
                }
            }
        }
    }


    private fun setupSubscribers() {
        EventOrchestrator.subscribe<DefaultGameEvents.GarbageSent, Int>(
            { lines -> lines?.let { receiveGarbage(it) } },
            { event -> if (event.gameId != gameId) event.lines else null }
        )
        EventOrchestrator.subscribe<DefaultGameEvents.GameOver> {
            isGameOver = true
        }
    }

    fun receiveGarbage(lines: Int) {
        scope.launch {
            queueMutex.withLock {
                val packet = GarbagePacket(lines, System.currentTimeMillis() + delayMillis)
                garbageQueue.add(packet)
            }
            updatePendingGarbage()
        }
    }

    fun sendGarbage(lines: Int, distributionMode: String) {
        scope.launch {
            var remaining = lines
            queueMutex.withLock {
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
            }

            if (remaining > 0) {
                EventOrchestrator.publish(DefaultGameEvents.GarbageSent(remaining, distributionMode, gameId))
            }
            updatePendingGarbage()
        }
    }

    private fun applyGarbage(packet: GarbagePacket) {
        EventOrchestrator.publish(DefaultGameEvents.GarbageReceived(packet.lines, gameId))
    }

    private fun updatePendingGarbage() {
        val pendingLines = garbageQueue.sumOf { it.lines }
        EventOrchestrator.publish(PendingGarbage(pendingLines, gameId))
    }
}
