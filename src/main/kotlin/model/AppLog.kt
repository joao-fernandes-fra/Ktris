package model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object AppLog {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    var minLevel = Level.DEBUG

    enum class Level { DEBUG, INFO, WARN, ERROR }

    fun <T> debug(tag: String = "APP", msg: () -> T) = log(Level.DEBUG, tag, msg())
    fun <T> debug(tag: String = "APP", msg: T) = log(Level.DEBUG, tag, msg)
    fun <T> info(tag: String = "APP", msg: T) = log(Level.INFO, tag, msg)
    fun <T> warn(tag: String = "APP", msg: T) = log(Level.WARN, tag, msg)
    fun <T> error(tag: String = "APP", msg: T) = log(Level.ERROR, tag, msg)

    private fun <T> log(level: Level, tag: String, message: T) {
        if (level.ordinal >= minLevel.ordinal) {
            val time = LocalDateTime.now().format(formatter)
            val output = "[$time] [$level] [$tag] - $message"
            if (level == Level.ERROR) System.err.println(output)
            else println(output)
        }
    }
}