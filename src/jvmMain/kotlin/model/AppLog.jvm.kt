package model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

actual object AppLog {
    actual var minLogLevel: LogLevel = LogLevel.DEBUG
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    actual fun log(logLevel: LogLevel, tag: String, message: () -> Any) {
        if (logLevel.ordinal >= minLogLevel.ordinal) {
            val timestamp = LocalDateTime.now().format(formatter)
            println("$timestamp [$tag][$logLevel] ${message()}")
        }
    }
}