package engine.controller.defaults

import engine.controller.GameRenderer
import engine.model.Drop
import engine.model.KtrisContext
import engine.model.Movement
import engine.model.Piece
import engine.model.Rotation
import engine.model.SpinType
import engine.model.TimeState
import engine.model.defaults.Logger
import engine.model.events.Event
import engine.model.events.EventOrchestrator
import engine.model.events.GameEvent.GarbageReceived
import engine.model.events.GameEvent.LevelUp
import engine.model.events.GameEvent.LineCleared
import engine.model.events.InputEvent.CommandInput
import engine.model.events.InputEvent.DirectionMoveEnd
import engine.model.events.InputEvent.DirectionMoveStart
import engine.model.events.InputEvent.DropInput
import engine.model.events.InputEvent.FreezeTime
import engine.model.events.InputEvent.RotationInputRelease
import engine.model.events.InputEvent.RotationInputStart
import engine.model.events.InputEvent.SlowDownTime
import kotlin.time.Clock
import kotlin.time.DurationUnit

open class BaseTetris<T : Piece>(
    context: KtrisContext<T>
) : DefaultTetrisEngine<T>(
    context.playerSettings,
    context.gameSettings,
    context.boardManager,
    context.pieceController,
    context.gameTimers,
    context.timeManager,
) {
    override val gameId: String = context.gameId
    private var freezeLineClears: Int = 0

    init {
        setupTimeSystem()
        setupInputEvents()
        setupGameEvents()
    }

    private fun setupTimeSystem() {
        timeManager.onFreezeEnded = {
            freezeLineClears = 0
            val linesCleared = boardManager.clearFullLines()

            if (linesCleared.isNotEmpty()) {
                EventOrchestrator.publish(
                    LineCleared(SpinType.NONE, linesCleared, boardManager.isBoardEmpty, gameId)
                )
            }
            Logger.info { "Freeze ended. Cleared $linesCleared lines immediately." }
        }
    }

    private fun setupGameEvents() {
        subscribeForGame<LevelUp, Int>(::levelUp) { it.newLevel }
        subscribeForGame<GarbageReceived, Int>({ lines ->
            processGarbage(lines)
        }, {
            it.lines
        })
    }

    private fun setupInputEvents() {
        EventOrchestrator.subscribeForGameId<CommandInput>(gameId) { onHold() }
        subscribeForGame<DirectionMoveStart, Movement>(::onMovement) { it.movement }
        subscribeForGame<DirectionMoveEnd, Movement>(::onMovementRelease) { it.movement }
        subscribeForGame<DropInput, Drop>(::onDrop) { it.dropType }
        subscribeForGame<RotationInputStart, Rotation>(::onRotation) { it.rotation }
        subscribeForGame<RotationInputRelease, Rotation>(::onRotationRelease) { it.rotation }
        subscribeForGame<SlowDownTime, Double>(
            { duration -> onTimeState(TimeState.SLOWED, duration) },
            { it.duration }
        )
        subscribeForGame<FreezeTime, Double>(
            { duration -> onTimeState(TimeState.FROZEN, duration) },
            { it.duration }
        )
    }

    override fun start(renderer: GameRenderer<T>) {
        var lastTime = Clock.System.now()

        while (!isGameOver || !isGoalMet) {
            val now = Clock.System.now()
            val delta = now - lastTime
            lastTime = now
            update(delta.toDouble(DurationUnit.MILLISECONDS))
            renderer.render(gameStateSnapshot())
        }
    }

    private inline fun <reified E : Event, V> subscribeForGame(
        crossinline handler: (V) -> Unit,
        crossinline extractor: (E) -> V?
    ) {
        EventOrchestrator.subscribe<E, V>(
            { value ->
                if (value != null) {
                    handler(value)
                }
            },
            { event ->
                if (event.gameId == gameId) extractor(event) else null
            }
        )
    }
}