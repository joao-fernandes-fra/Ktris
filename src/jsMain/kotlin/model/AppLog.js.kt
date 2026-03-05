package model

import kotlin.js.Date

actual object AppLog {
    actual var minLogLevel: LogLevel = LogLevel.DEBUG

    actual fun log(logLevel: LogLevel, tag: String, message: () -> Any) {
        if (logLevel.ordinal >= minLogLevel.ordinal) {
            val timestamp = Date().toLocaleString()
            println("$timestamp [$tag][$logLevel] ${message()}")
        }
    }
}
