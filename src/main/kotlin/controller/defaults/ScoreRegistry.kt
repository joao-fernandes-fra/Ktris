package controller.defaults

import controller.ScoringRuleBook
import model.AppLog
import model.Drop
import model.SpinType
import model.events.EventHandler
import model.events.EventHandler.publish
import model.events.GameEvent.BackToBackTrigger
import model.events.GameEvent.ComboTriggered
import model.events.GameEvent.HardDrop
import model.events.GameEvent.LevelUp
import model.events.GameEvent.LineCleared
import model.events.GameEvent.ScoreUpdated
import model.events.GameEvent.SoftDrop

class ScoreRegistry(private val ruleBook: ScoringRuleBook) {

    init {
        setupEventListener()
    }

    private fun setupEventListener() {
        EventHandler.subscribeToEvent<LineCleared> { event ->
            recordAction(event.spinType, event.linesCleared, event.isPerfectClear)
        }
        EventHandler.subscribeToEvent<HardDrop> { event ->
            recordDrop(Drop.HARD_DROP, event.distance)
        }
        EventHandler.subscribeToEvent<SoftDrop> { event ->
            recordDrop(Drop.SOFT_DROP, event.distance)
        }
    }

    private var level: Int = 1
    var totalLinesCleared: Int = 0; private set
    var totalPoints: Double = 0.0; private set
    var combo: Int = -1; private set
    var b2bCount: Int = -1; private set

    private fun recordAction(action: SpinType, lines: Int, isPerfectClear: Boolean) {
        AppLog.debug { "Recording action $action" }
        val moveType = ruleBook.getMoveType(action, lines)
        var basePoints = ruleBook.getBasePoints(action, lines)

        if (ruleBook.isDifficult(action, lines)) {
            b2bCount++
            if (b2bCount > 0) {
                publish(BackToBackTrigger.topic, BackToBackTrigger(b2bCount))
                basePoints *= 1.5
            }
        } else {
            b2bCount = -1
        }

        if (lines > 0) combo++ else combo = -1

        val comboBonus = if (combo > 0) {
            publish(ComboTriggered.topic, ComboTriggered(combo))
            (ruleBook.comboFactor * combo * level)
        } else 0.0

        val pcBonus = if (isPerfectClear) ruleBook.perfectClearBonus * level else 0.0

        val pointsAwarded = (basePoints * level) + comboBonus + pcBonus
        totalPoints += pointsAwarded
        totalLinesCleared += lines
        handleLevelUp()
        AppLog.info { "Score Updated: $totalPoints (Total Lines: $totalLinesCleared) " + if (moveType.isSpecial) "(SpecialMove: $moveType)" else "" }
        publish(
            ScoreUpdated.topic,
            ScoreUpdated(
                totalLinesCleared,
                totalPoints,
                pointsAwarded,
                 moveType.displayName.takeIf { moveType.isSpecial },
                combo,
                b2bCount
            )
       )
    }

    private fun handleLevelUp() {
        val newLevel = totalLinesCleared.div(10)
        if (newLevel > level) {
            AppLog.info { "Level Up Trigger [from $level to $newLevel]" }
            publish(LevelUp.topic, LevelUp(newLevel))
        }
        level = newLevel
    }

    private fun recordDrop(type: Drop, distance: Int) {
        val dropPoints = distance * (ruleBook.dropTables[type] ?: 0.0)
        AppLog.debug { "Drop Trigger [$type] awarded: $dropPoints points" }
        totalPoints += dropPoints
    }
}