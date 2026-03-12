package engine.model.events


object EventOrchestrator {
    val listeners = mutableListOf<(Event) -> Unit>()

    fun publish(event: Event) {
        listeners.toList().forEach { it(event) }
    }

    inline fun <reified T : Event> subscribe(crossinline callback: (T) -> Unit) {
        listeners.add { if (it is T) callback(it) }
    }

    inline fun <reified T : Event> subscribeForGameId(
        gameId: String,
        crossinline callback: (T) -> Unit
    ) {
        listeners.add { if (it is T && it.gameId == gameId) callback(it) }
    }

    inline fun <reified T : Event, M> subscribe(
        crossinline callback: (M?) -> Unit,
        crossinline extractor: (T) -> M?
    ) {
        listeners.add { if (it is T) callback(extractor(it)) }
    }

    fun unsubscribeAll() {
        listeners.clear()
    }
}
