package demo.model

import demo.controller.AttackSimulator
import demo.controller.GarbageProcessor
import engine.controller.GameRenderer
import engine.controller.defaults.BaseTetris
import engine.model.KtrisContext
import engine.model.SpinType
import engine.model.TimeState
import engine.model.defaults.Logger
import engine.model.defaults.ProceduralPiece
import engine.model.events.EventOrchestrator
import engine.model.events.GameEvent
import engine.model.events.GameId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class SwingTetris(context: KtrisContext<ProceduralPiece>) : BaseTetris<ProceduralPiece>(context) {

    private var isCheeseGame: Boolean = false
    private lateinit var garbageProcessor: GarbageProcessor

    init {
        EventOrchestrator.subscribeForGameId<ToggleFreeze>(gameId) {
            if (timeManager.isFrozen) {
                timeManager.transition(TimeState.Normal)
            } else timeManager.transition(TimeState.Frozen())
        }
        timeManager.onStateChanged = { from, to ->
            if (from is TimeState.Frozen && to is TimeState.Normal) {
                freezeLineClears = 0
                val linesCleared = boardManager.clearFullLines()

                if (linesCleared.isNotEmpty()) {
                    EventOrchestrator.publish(
                        GameEvent.LineCleared(
                            spinType = SpinType.NONE,
                            linesCleared = linesCleared,
                            isEmptyBoard = boardManager.isBoardEmpty,
                            gameId = gameId
                        )
                    )
                }
                Logger.info { "Freeze ended. Cleared $linesCleared lines immediately." }
            }
        }
    }

    fun initialize(isCheeseGame: Boolean, enemyApm: Int?, gameScope: CoroutineScope) {
        this.isCheeseGame = isCheeseGame
        garbageProcessor = GarbageProcessor(gameScope)
        setUpGarbageListeners()

        if (isCheeseGame && enemyApm != null) {
            val enemyScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + GameId(ENEMY_GAME_ID))
            AttackSimulator(enemyScope, enemyApm).startProcess()
        }
    }

    override fun start(renderer: GameRenderer<ProceduralPiece>) {
        val targetFrameTime = 16.67
        var lastTime = Clock.System.now()
        var accumulator = 0.0

        val timer = javax.swing.Timer(targetFrameTime.toInt()) { _ ->
            if (isGameOver || isGoalMet) return@Timer
            val now = Clock.System.now()
            val delta = (now - lastTime).toDouble(DurationUnit.MILLISECONDS)
            lastTime = now
            accumulator += delta.coerceAtMost(250.0)
            while (accumulator >= targetFrameTime) {
                update(targetFrameTime)
                accumulator -= targetFrameTime
            }
            renderer.render(gameStateSnapshot())
        }
        timer.isRepeats = true
        timer.start()
    }

    private fun setUpGarbageListeners() {
        EventOrchestrator.subscribe<GameEvent.LineCleared, Int>({ totalLines ->
            if (totalLines != null && totalLines > 0) {
                garbageProcessor.sendGarbage(totalLines, "all")
            }
        }, { event -> if (event.gameId == PLAYER_GAME_ID) event.linesCleared.size else null })
    }

    companion object {
        val PLAYER_GAME_ID = "Ktris-${Uuid.random().toHexString()}"
        val ENEMY_GAME_ID = "Enemy-${Uuid.random().toHexString()}"
    }
}
