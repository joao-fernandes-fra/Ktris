package engine.model.defaults

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private const val ANSI_RESET = "\u001B[0m"

private const val ANSI_RED = "\u001B[31m"

private const val ANSI_YELLOW = "\u001B[33m"

/**
 * Fun exercise to avoid using jvm specifics and keep the default logger field, nothing here is necessary
 * */
object AppLog {
    var minLevel = Level.INFO

    enum class Level { DEBUG, INFO, WARN, ERROR }

    fun debug(tag: String = "APP", msg: () -> Any) = log(Level.DEBUG, tag, msg)
    fun info(tag: String = "APP", msg: () -> Any) = log(Level.INFO, tag, msg)
    fun warn(tag: String = "APP", msg: () -> Any) = log(Level.WARN, tag, msg)
    fun error(tag: String = "APP", msg: () -> Any) = log(Level.ERROR, tag, msg)

    private fun log(level: Level, tag: String, message: () -> Any) {
        if (level.ordinal >= minLevel.ordinal) {
            val time: String = Clock.System.now().toLocalDateTime(TimeZone.UTC).format("HH:mm:ss.SSS")
            val output = "[$time] [$level] [$tag] - ${message()}"
            printLevel(output, level)
        }
    }

    private fun printLevel(text: String, level: Level) {
        return when (level) {
            Level.DEBUG -> println(text)
            Level.INFO -> println(text)
            Level.WARN -> println("$ANSI_YELLOW $text $ANSI_RESET")
            Level.ERROR -> println("$ANSI_RED $text $ANSI_RESET")
        }
    }

    private fun LocalDateTime.format(pattern: String): String {

        var accumulator = 0
        return pattern.asSequence().joinToString("") { token ->
            when (token) {
                'H' -> {
                    hour.collectChar(accumulator).apply { accumulator++ }
                }

                'm' -> {
                    minute.collectChar(accumulator).apply { accumulator++ }
                }

                's' -> {
                    second.collectChar(accumulator).apply { accumulator++ }
                }

                'S' -> {
                    nanosecond.collectChar(accumulator).apply { accumulator++ }
                }

                else -> {
                    accumulator = 0
                    token
                }
            }.toString()
        }

    }

    private fun Int.collectChar(index: Int): Char {
        val value = toString().getOrElse(index) { '0' }
        return value
    }
}