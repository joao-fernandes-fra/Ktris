package model

class ZoneLineBuffer(private val onClearTriggered: (Int) -> Unit) {
    private var accumulatedLines = 0

    fun recordClear(lineCount: Int) {
        accumulatedLines += lineCount
    }

    fun flush() {
        if (accumulatedLines > 0) {
            onClearTriggered(accumulatedLines)
            accumulatedLines = 0
        }
    }
}