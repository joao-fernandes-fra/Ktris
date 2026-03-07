package engine.controller.defaults

import engine.model.Drop
import engine.model.ScoringRuleBook
import engine.model.SpinType
import engine.model.events.EventOrchestrator
import engine.model.events.GameEvent.BackToBackTrigger
import engine.model.events.GameEvent.ComboTriggered
import engine.model.events.GameEvent.HardDrop
import engine.model.events.GameEvent.LevelUp
import engine.model.events.GameEvent.LineCleared
import engine.model.events.GameEvent.ScoreUpdated
import engine.model.events.GameEvent.SoftDrop
import engine.model.events.GameId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ScoreTracker(
    private val ruleBook: ScoringRuleBook,
    private val scope: CoroutineScope
) {
    val gameId: String = scope.coroutineContext[GameId]?.value ?: error("No gameId in context")

    init {
        setupEventListener()
    }

    private fun setupEventListener() {
        EventOrchestrator.subscribe<LineCleared> { event ->
            recordAction(event.spinType, event.linesCleared.size, event.isEmptyBoard)
        }
        scope.launch {
            EventOrchestrator.subscribe<HardDrop> { event ->
                recordDrop(Drop.HARD_DROP, event.distance)
            }
        }
        scope.launch {
            EventOrchestrator.subscribe<SoftDrop> { event ->
                recordDrop(Drop.SOFT_DROP, event.distance)
            }
        }
    }

    private var level: Int = 1
    var totalLinesCleared: Int = 0; private set
    var totalPoints: Double = 0.0; private set
    var combo: Int = -1; private set
    var b2bCount: Int = -1; private set

    private suspend fun recordAction(action: SpinType, lines: Int, isBoardEmpty: Boolean) {
        val moveType = ruleBook.getMoveType(action, lines)
        var basePoints = ruleBook.getBasePoints(action, lines)

        if (ruleBook.isDifficult(action, lines)) {
            b2bCount++
            if (b2bCount > 0) {
                EventOrchestrator.publish(BackToBackTrigger(b2bCount, gameId))
                basePoints *= 1.5
            }
        } else {
            b2bCount = -1
        }

        if (lines > 0) combo++ else combo = -1

        val comboBonus = if (combo > 0) {
            EventOrchestrator.publish(ComboTriggered(combo, gameId))
            (ruleBook.comboFactor * combo * level)
        } else 0.0

        val pcBonus = if (isBoardEmpty) ruleBook.perfectClearBonus * level else 0.0

        val pointsAwarded = (basePoints * level) + comboBonus + pcBonus
        totalPoints += pointsAwarded
        totalLinesCleared += lines
        handleLevelUp()
        EventOrchestrator.publish(
            ScoreUpdated(totalLinesCleared, totalPoints, pointsAwarded, moveType, gameId)
        )
    }

    private suspend fun handleLevelUp() {
        val newLevel = totalLinesCleared / 10
        if (newLevel > level) {
            EventOrchestrator.publish(LevelUp(newLevel, gameId))
        }
        level = newLevel
    }

    private fun recordDrop(type: Drop, distance: Int) {
        val dropPoints = distance * (ruleBook.dropTables[type] ?: 0.0)
        totalPoints += dropPoints
    }
}
