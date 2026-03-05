package model.events

import java.util.concurrent.Executors

actual class EventExecutor {
    private val executor = Executors.newSingleThreadExecutor()
    actual fun execute(task: () -> Unit) {
        executor.submit { task() }
    }

    actual fun shutdown() = executor.shutdown()
}