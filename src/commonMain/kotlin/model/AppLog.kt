package model

enum class Level { DEBUG, INFO, WARN, ERROR }

expect object AppLog {

    var minLevel: Level

    fun debug(tag: String = "APP", msg: () -> Any) = log(Level.DEBUG, tag, msg)
    fun info(tag: String = "APP", msg: () -> Any) = log(Level.INFO, tag, msg)
    fun warn(tag: String = "APP", msg: () -> Any) = log(Level.WARN, tag, msg)
    fun error(tag: String = "APP", msg: () -> Any) = log(Level.ERROR, tag, msg)

    fun log(level: Level, tag: String, message: () -> Any)
}