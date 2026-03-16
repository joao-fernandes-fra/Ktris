package demo.model

import demo.controller.AttackSimulator
import demo.controller.GarbageProcessor
import engine.controller.GameRenderer
import engine.controller.defaults.DefaultGameEngine
import engine.model.KtrisContext
import engine.model.SpinType
import engine.model.TimeState
import engine.util.Logger
import engine.model.defaults.ProceduralPiece
import engine.model.events.DefaultGameEvents
import engine.model.events.EventOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.swing.Timer
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class SwingTetris(
    context: KtrisContext<ProceduralPiece>,
    override val gameId: String = context.gameId
) : DefaultGameEngine<ProceduralPiece>(
    context.playerSettings,
    context.gameSettings,
    context.boardManager,
    context.pieceController,
    context.scoringEngine
) {

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
                        DefaultGameEvents.LineCleared(
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
        garbageProcessor = GarbageProcessor(gameScope, 20, PLAYER_GAME_ID)
        setUpGarbageListeners()

        if (isCheeseGame && enemyApm != null) {
            val enemyScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            AttackSimulator(enemyScope, enemyApm).startProcess()
        }
    }

    fun start(renderer: GameRenderer<ProceduralPiece>) {
        val targetFrameTime = 16.67
        var lastTime = Clock.System.now()
        var accumulator = 0.0

        val timer = Timer(targetFrameTime.toInt()) { _ ->
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
        EventOrchestrator.subscribeForGameId<DefaultGameEvents.LineCleared>(gameId) { event ->
            if (event.linesCleared.isNotEmpty()) {
                garbageProcessor.sendGarbage(event.linesCleared.size, "all")
            }
        }
        EventOrchestrator.subscribeForGameId<DefaultGameEvents.GarbageReceived>(gameId) { event ->
            processGarbage(event.lines)
        }
    }

    companion object {
        val PLAYER_GAME_ID = "Ktris-${Uuid.random().toHexString()}"
        val ENEMY_GAME_ID = "Enemy-${Uuid.random().toHexString()}"
    }
}
