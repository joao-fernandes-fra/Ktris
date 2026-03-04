package model.events

import kotlinx.browser.window

actual class EventExecutor {
    actual fun execute(task: () -> Unit) {
        window.setTimeout({ task() }, 0)
    }
    actual fun shutdown() { /* no-op */ }
}