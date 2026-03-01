package model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object AppLog {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    var minLevel = Level.INFO

    enum class Level { DEBUG, INFO, WARN, ERROR }

    fun debug(tag: String = "APP", msg: () -> Any) = log(Level.DEBUG, tag, msg)
    fun info(tag: String = "APP", msg: () -> Any) = log(Level.INFO, tag, msg)
    fun warn(tag: String = "APP", msg: () -> Any) = log(Level.WARN, tag, msg)
    fun error(tag: String = "APP", msg: () -> Any) = log(Level.ERROR, tag, msg)

    private fun log(level: Level, tag: String, message: () -> Any) {
        if (level.ordinal >= minLevel.ordinal) {
            val time = LocalDateTime.now().format(formatter)
            val output = "[$time] [$level] [$tag] - ${message()}"
            if (level == Level.ERROR) System.err.println(output) else println(output)
        }
    }
}