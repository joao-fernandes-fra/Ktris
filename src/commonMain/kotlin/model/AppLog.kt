package model

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

expect object AppLog {

    var minLogLevel: LogLevel

    fun log(logLevel: LogLevel, tag: String, message: () -> Any)
}

fun AppLog.debug(tag: String = "APP", msg: () -> Any) = log(LogLevel.DEBUG, tag, msg)
fun AppLog.info(tag: String = "APP", msg: () -> Any) = log(LogLevel.INFO, tag, msg)
fun AppLog.warn(tag: String = "APP", msg: () -> Any) = log(LogLevel.WARN, tag, msg)
fun AppLog.error(tag: String = "APP", msg: () -> Any) = log(LogLevel.ERROR, tag, msg)