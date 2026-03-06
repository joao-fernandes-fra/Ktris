package demo.model

import engine.model.events.Event

data class PlayerAPMUpdated(val apm: Float, override val gameId: String) : Event
data class PendingGarbage(val lines: Int, override val gameId: String) : Event