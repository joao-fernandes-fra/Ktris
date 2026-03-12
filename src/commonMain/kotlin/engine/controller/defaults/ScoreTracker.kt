package engine.controller.defaults

import engine.model.Drop
import engine.model.Resetable
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
import engine.model.events.InputEvent

class ScoreTracker(
    private val ruleBook: ScoringRuleBook,
    private val gameId: String
) : Resetable {

    var level: Int = ruleBook.startingLevel; private set
    var totalLinesCleared: Int = 0; private set
    var totalPoints: Double = 0.0; private set
    var combo: Int = -1; private set
    var b2bCount: Int = -1; private set

    init {
        setupEventListeners()
    }

    private fun setupEventListeners() {
        EventOrchestrator.subscribeForGameId<LineCleared>(gameId) { event ->
            recordAction(event.spinType, event.linesCleared.size, event.isEmptyBoard, event.piece?.name ?: "")
        }
        EventOrchestrator.subscribeForGameId<HardDrop>(gameId) { event ->
            recordDrop(Drop.HARD_DROP, event.distance)
        }
        EventOrchestrator.subscribeForGameId<SoftDrop>(gameId) { event ->
            recordDrop(Drop.SOFT_DROP, event.distance)
        }
        EventOrchestrator.subscribeForGameId<InputEvent.ResetInput>(gameId) {
            reset()
        }
        EventOrchestrator.subscribeForGameId<LevelUp>(gameId) { event ->
            level = event.newLevel
        }
    }

    private fun recordAction(spinType: SpinType, lines: Int, isBoardEmpty: Boolean, pieceName: String) {
        val moveType = ruleBook.getMoveType(spinType, lines, pieceName)
        var basePoints = ruleBook.getBasePoints(spinType, lines)

        if (ruleBook.isDifficult(spinType, lines)) {
            b2bCount++
            if (b2bCount > 0) {
                EventOrchestrator.publish(BackToBackTrigger(b2bCount, gameId))
                basePoints *= ruleBook.b2bMultiplier
            }
        } else if (lines > 0) {
            b2bCount = -1
        }

        if (lines > 0) {
            combo++
            if (combo > 0) EventOrchestrator.publish(ComboTriggered(combo, gameId))
        } else {
            combo = -1
        }

        val comboBonus = if (combo > 0) ruleBook.comboFactor * combo * level else 0.0
        val pcBonus = if (isBoardEmpty) ruleBook.perfectClearBonus * level else 0.0
        val pointsAwarded = (basePoints * level) + comboBonus + pcBonus

        totalPoints += pointsAwarded
        totalLinesCleared += lines

        handleLevelUp()

        EventOrchestrator.publish(
            ScoreUpdated(totalLinesCleared, totalPoints, pointsAwarded, moveType, gameId)
        )
    }

    private fun handleLevelUp() {
        val newLevel = ruleBook.levelForLines(totalLinesCleared, level)
        if (newLevel > level) {
            level = newLevel
            EventOrchestrator.publish(LevelUp(newLevel, gameId))
        }
    }

    private fun recordDrop(type: Drop, distance: Int) {
        val dropPoints = distance * (ruleBook.dropTables[type] ?: 0.0)
        totalPoints += dropPoints
    }

    override fun reset() {
        level = ruleBook.startingLevel
        totalLinesCleared = 0
        totalPoints = 0.0
        combo = -1
        b2bCount = -1
    }
}