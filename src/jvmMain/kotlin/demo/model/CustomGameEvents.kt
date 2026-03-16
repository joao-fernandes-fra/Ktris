package demo.model

import engine.model.events.GameEvent

data class PlayerAPMUpdated(val apm: Float, override val gameId: String) : GameEvent
data class PendingGarbage(val lines: Int, override val gameId: String) : GameEvent

data class ToggleFreeze(override val gameId: String) : GameEvent