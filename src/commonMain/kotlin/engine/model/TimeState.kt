package engine.model

sealed class TimeState {
    object Normal : TimeState()

    data class Slowed(
        val multiplier: Double,
        val durationMs: Double? = null
    ) : TimeState()

    data class Frozen(
        val durationMs: Double? = null
    ) : TimeState()
}
