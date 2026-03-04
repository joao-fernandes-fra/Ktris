package model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

actual object AppLog {
    actual var minLevel: Level = Level.DEBUG
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    actual fun log(level: Level, tag: String,  message: () -> Any) {
        if (level.ordinal >= minLevel.ordinal) {
            val timestamp = LocalDateTime.now().format(formatter)
            println("$timestamp [$tag][$level] ${message()}")
        }
    }
}