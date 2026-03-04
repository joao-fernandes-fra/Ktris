package model

import kotlin.js.Date

actual object AppLog {
    actual var minLevel: Level = Level.DEBUG

    actual fun log(level: Level, tag: String, message: () -> Any) {
        if (level.ordinal >= minLevel.ordinal) {
            val timestamp = Date().toLocaleString()
            println("$timestamp [$tag][$level] ${message()}")
        }
    }
}
