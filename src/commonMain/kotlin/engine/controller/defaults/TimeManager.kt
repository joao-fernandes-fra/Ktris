package engine.controller.defaults

import engine.model.Resetable
import engine.model.TimeState
import engine.model.defaults.Logger

class TimeManager : Resetable {

    var state: TimeState = TimeState.Normal
        private set

    var onStateChanged: ((from: TimeState, to: TimeState) -> Unit)? = null

    val isFrozen: Boolean get() = state is TimeState.Frozen
    val stateProgress: Double?
        get() {
            val s = state
            val total = when (s) {
                is TimeState.Slowed -> s.durationMs ?: return null
                is TimeState.Frozen -> s.durationMs ?: return null
                is TimeState.Normal -> return null
            }
            return (1.0 - (remainingMs / total)).coerceIn(0.0, 1.0)
        }

    var remainingMs: Double = 0.0
        private set

    fun transition(newState: TimeState) {
        val previous = state
        state = newState
        remainingMs = when (newState) {
            is TimeState.Slowed -> newState.durationMs ?: Double.MAX_VALUE
            is TimeState.Frozen -> newState.durationMs ?: Double.MAX_VALUE
            is TimeState.Normal -> 0.0
        }
        if (previous != newState) {
            onStateChanged?.invoke(previous, newState)
            Logger.info { "TimeManager: $previous → $newState" }
        }
    }

    fun tick(deltaTime: Double): Double {
        return when (val s = state) {
            is TimeState.Normal -> deltaTime

            is TimeState.Slowed -> {
                if (s.durationMs != null) {
                    remainingMs -= deltaTime
                    if (remainingMs <= 0.0) transition(TimeState.Normal)
                }
                deltaTime * s.multiplier
            }

            is TimeState.Frozen -> {
                if (s.durationMs != null) {
                    remainingMs -= deltaTime
                    if (remainingMs <= 0.0) transition(TimeState.Normal)
                }
                0.0
            }
        }
    }

    override fun reset() {
        state = TimeState.Normal
        remainingMs = 0.0
    }
}