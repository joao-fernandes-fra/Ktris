package engine.controller.defaults.scoring

import engine.model.Resetable
import engine.model.events.DefaultGameEvents.ScoreUpdated
import engine.model.events.EventOrchestrator

class ScoreTracker(
    private val gameId: String
) : Resetable {
    var totalPoints: Double = 0.0; private set

    init {
        subscribe()
    }

    private fun subscribe() {
        EventOrchestrator.subscribeForGameId<ScoreUpdated>(gameId) { event ->
            totalPoints += event.pointsEarned
        }
    }

    override fun reset() {
        totalPoints = 0.0
    }
}