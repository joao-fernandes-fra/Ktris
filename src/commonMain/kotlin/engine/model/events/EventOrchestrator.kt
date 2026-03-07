package engine.model.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch


object EventOrchestrator {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 64)
    val events: SharedFlow<Event> = _events

    fun publish(event: Event) {
        _events.tryEmit(event)
    }

    inline fun <reified T : Event> subscribe(crossinline callback: suspend (T) -> Unit) {
        scope.launch {
            events.filterIsInstance<T>().collect { callback(it) }
        }
    }

    inline fun <reified T : Event, M> subscribe(
        crossinline callback: suspend (M?) -> Any?, crossinline extractor: suspend (T) -> M?
    ) {
        scope.launch {
            events.filterIsInstance<T>().collect {
                callback(extractor(it))
            }
        }
    }

}
