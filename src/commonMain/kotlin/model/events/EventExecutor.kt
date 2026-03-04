package model.events

expect class EventExecutor() {
    fun execute(task: () -> Unit)
    fun shutdown()
}