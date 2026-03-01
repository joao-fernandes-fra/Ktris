package model


class ScoreRegistry(private val ruleBook: ScoringRuleBook, private val gameEventBus: GameEventBus) {

    init {
        gameEventBus.subscribe<GameEvent.LineCleared> { event ->
            recordAction(PieceAction.mapAction(event.spinType), event.linesCleared, event.isPerfectClear)
        }

        gameEventBus.subscribe<GameEvent.HardDrop> { event ->
            recordDrop(Drop.HARD_DROP, event.distance)
        }

        gameEventBus.subscribe<GameEvent.SoftDrop> { event ->
            recordDrop(Drop.SOFT_DROP, event.distance)
        }

        gameEventBus.subscribe<GameEvent.LevelUp> { event ->
            level = event.newLevel
        }
    }

    private var level: Int = 1
    var totalLinesCleared: Int = 0; private set
    var totalPoints: Double = 0.0; private set
    var combo: Int = -1; private set
    var b2bCount: Int = -1; private set

    private fun recordAction(action: PieceAction, lines: Int, isPerfectClear: Boolean) {
        AppLog.debug { "Recording action $action" }
        val moveType = ruleBook.getMoveType(action, lines)
        var basePoints = ruleBook.getBasePoints(action, lines)

        if (ruleBook.isDifficult(action, lines)) {
            b2bCount++
            if (b2bCount > 0) {
                gameEventBus.post(GameEvent.BackToBackTrigger(b2bCount))
                basePoints *= 1.5
            }
        } else if (lines > 0) {
            b2bCount = -1
        }

        if (lines > 0) combo++ else combo = -1

        val comboBonus = if (combo > 0) {
            gameEventBus.post(GameEvent.ComboTriggered(combo))
            (ruleBook.comboFactor * combo * level)
        } else 0.0

        val pcBonus = if (isPerfectClear) ruleBook.perfectClearBonus * level else 0.0

        val pointsAwarded = (basePoints * level) + comboBonus + pcBonus
        totalPoints += pointsAwarded
        totalLinesCleared += lines
        AppLog.info { "Score Updated: $totalPoints (Total Lines: $totalLinesCleared) " + if (moveType.isSpecial) "(SpecialMove: $moveType)" else "" }
        gameEventBus.post(GameEvent.ScoreUpdated(totalLinesCleared, totalPoints, pointsAwarded, moveType))
    }

    private fun recordDrop(type: Drop, distance: Int) {
        totalPoints += distance * (ruleBook.dropTables[type] ?: 0.0)
    }
}