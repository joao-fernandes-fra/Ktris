package engine.controller.defaults

import engine.model.defaults.AppLog
import engine.model.GlobalGameSettings
import engine.model.Resetable
import engine.model.TimeState

class TimeManager(private val settings: GlobalGameSettings) : Resetable {
    var mode: TimeState = TimeState.NORMAL
        private set

    var onFreezeEnded: (() -> Unit)? = null

    private var modeDuration: Double = 0.0

    fun onState(state: TimeState, duration: Double) {
        when (state) {
            TimeState.NORMAL -> resetState()
            TimeState.FROZEN -> freezeTime(duration)
            TimeState.SLOWED -> slowDownTime(duration)
        }
    }

    private fun freezeTime(duration: Double) {
        AppLog.info { "Freezing time: $duration" }
        mode = TimeState.FROZEN
        modeDuration = duration
    }

    private fun resetState() {
        mode = TimeState.NORMAL
        onFreezeEnded?.invoke()
        AppLog.info { "Returning to normal state: $mode" }
    }

    fun slowDownTime(duration: Double) {
        mode = TimeState.SLOWED
        modeDuration = duration
    }

    fun tick(deltaTime: Double): Double {
        if (mode == TimeState.NORMAL) return deltaTime

        if (mode == TimeState.SLOWED) {
            return deltaTime * settings.slowDownMultiplier
        }

        modeDuration -= deltaTime
        if (modeDuration <= 0) {
            resetState()
        }

        return 0.0
    }

    override fun reset() {
        mode = TimeState.NORMAL
        modeDuration = 0.0
    }
}