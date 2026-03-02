package controller.defaults

import model.AppLog
import model.GameConfig
import model.TimeMode

class TimeManager(private val settings: GameConfig) {
    var mode: TimeMode = TimeMode.NORMAL
        private set

    var onFreezeEnded: (() -> Unit)? = null

    private var modeDuration: Float = 0f

    fun freezeTime(duration: Float) {
        AppLog.info { "Freezing time: $duration" }
        mode = TimeMode.FROZEN
        modeDuration = duration
    }

    fun resetState() {
        mode = TimeMode.NORMAL
        onFreezeEnded?.invoke()
        AppLog.info { "Returning to normal state: $mode" }
    }

    fun slowDownTime(duration: Float) {
        mode = TimeMode.SLOWED
        modeDuration = duration
    }

    fun tick(deltaTime: Float): Float {
        if (mode == TimeMode.NORMAL) return deltaTime

        if (mode == TimeMode.SLOWED) {
            return deltaTime * settings.slowDownMultiplier
        }

        modeDuration -= deltaTime
        if (modeDuration <= 0) {
            resetState()
        }

        return 0f
    }
}