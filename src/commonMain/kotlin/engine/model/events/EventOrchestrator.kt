package engine.model.events


object EventOrchestrator {
    val listeners = mutableListOf<(GameEvent) -> Unit>()

    fun publish(event: GameEvent) {
        listeners.toList().forEach { it(event) }
    }

    inline fun <reified T : GameEvent> subscribe(crossinline callback: (T) -> Unit) {
        listeners.add { if (it is T) callback(it) }
    }

    inline fun <reified T : GameEvent> subscribeForGameId(
        gameId: String,
        crossinline callback: (T) -> Unit
    ) {
        listeners.add { if (it is T && it.gameId == gameId) callback(it) }
    }

    inline fun <reified T : GameEvent, M> subscribe(
        crossinline callback: (M?) -> Unit,
        crossinline extractor: (T) -> M?
    ) {
        listeners.add { if (it is T) callback(extractor(it)) }
    }

    fun <T : GameEvent> unsubscribe(callback: (T) -> Unit) {
        listeners.remove(callback)
    }

    fun unsubscribeAll() {
        listeners.clear()
    }
}
