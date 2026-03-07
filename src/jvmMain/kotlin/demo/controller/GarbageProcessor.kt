package demo.controller

import demo.model.PendingGarbage
import engine.model.Command
import engine.model.events.EventOrchestrator
import engine.model.events.GameEvent
import engine.model.events.GameId
import engine.model.events.InputEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val GARBAGE_ENTRANCE_DELAY = 1000L

class GarbageProcessor(
    private val scope: CoroutineScope,
    private val delayMillis: Long = GARBAGE_ENTRANCE_DELAY
) {
    private data class GarbagePacket(val lines: Int, val scheduledAt: Long)

    private val queueMutex = Mutex()
    private val garbageQueue = ArrayDeque<GarbagePacket>()
    private val gameId = scope.coroutineContext[GameId]?.value ?: error("No GameId in scope")
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
        EventOrchestrator.subscribe<GameEvent.GarbageSent, Int>(
            { lines -> lines?.let { receiveGarbage(it) } },
            { event -> if (event.gameId != gameId) event.lines else null }
        )
        EventOrchestrator.subscribe<GameEvent.GameOver> {
            isGameOver = true
        }
        EventOrchestrator.subscribe<InputEvent.CommandInput> {
            if (it.command == Command.RESET && isGameOver) {
                isGameOver = false
                startProcessing()
            }
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
                EventOrchestrator.publish(GameEvent.GarbageSent(remaining, distributionMode, gameId))
            }
            updatePendingGarbage()
        }
    }

    private fun applyGarbage(packet: GarbagePacket) {
        EventOrchestrator.publish(GameEvent.GarbageReceived(packet.lines, gameId))
    }

    private suspend fun updatePendingGarbage() {
        val pendingLines = queueMutex.withLock {
            garbageQueue.sumOf { it.lines }
        }
        EventOrchestrator.publish(PendingGarbage(pendingLines, gameId))
    }
}
