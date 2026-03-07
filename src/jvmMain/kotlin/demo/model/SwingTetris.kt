package demo.model

import demo.controller.AttackSimulator
import demo.controller.GarbageProcessor
import demo.controller.SwingInputHandler
import demo.view.SwingRenderer
import engine.controller.defaults.BaseTetris
import engine.controller.defaults.GameRegistry
import engine.controller.defaults.ScoreProvider
import engine.model.defaults.Logger
import engine.model.GameGoal
import engine.model.defaults.ProceduralPiece
import engine.model.defaults.Tetromino
import engine.model.events.EventOrchestrator
import engine.model.events.GameEvent
import engine.model.events.GameId
import engine.util.GameSettingsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.swing.JFrame
import javax.swing.WindowConstants
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class SwingTetris(
    args: Array<String>
) {
    companion object {
        val PLAYER_GAME_ID = "Ktris-${Uuid.random().toHexString()}"
        val ENEMY_GAME_ID = "Enemy-${Uuid.random().toHexString()}"
    }

    private var isCheeseGame: Boolean = false
    private var frame: JFrame
    private var renderer: SwingRenderer<ProceduralPiece>
    private var garbageProcessor: GarbageProcessor

    init {
        Logger.minLevel = Logger.Level.DEBUG

        val (playerSetting, globalSettings) = when {
            args.contains("expert") -> GameSettingsProvider.expert()
            args.contains("pro") -> GameSettingsProvider.pro()
            else -> GameSettingsProvider.normal()
        }
        isCheeseGame = args.contains("versus")

        val gameSettings = if (args.contains("4way"))
            globalSettings.copy(goalType = GameGoal.TIME, goalValue = 2 * 60.0, boardCols = 4)
        else
            globalSettings.copy(goalType = GameGoal.TIME, goalValue = 2 * 60.0)

        frame = JFrame("Ktris - ${if (args.isEmpty()) "Normal" else args[0].uppercase()}")

        val gameContext = GameRegistry.buildContext(gameSettings, playerSetting, Tetromino.pieces, PLAYER_GAME_ID)
        val game = GameRegistry.createGame(gameContext)

        ScoreProvider.defaultBuilder(PLAYER_GAME_ID)
            .withScope(game.scope)
            .build()
        val inputHandler = SwingInputHandler(game.scope)
        renderer = SwingRenderer(game.scope)

        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        frame.add(renderer)
        frame.addKeyListener(inputHandler)
        frame.pack()
        frame.isVisible = true

        if (isCheeseGame) {
            val apm = when {
                args.contains("expert") -> 30
                args.contains("pro") -> 60
                else -> 10
            }
            val enemyScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + GameId(ENEMY_GAME_ID))
            AttackSimulator(enemyScope, apm).startProcess()
        }

        garbageProcessor = GarbageProcessor(game.scope)
        setUpGarbageListeners()
    }

    suspend fun run() {
        val gameScope = GameRegistry.get<ProceduralPiece>(PLAYER_GAME_ID).scope
        BaseTetris<ProceduralPiece>(gameScope).start(renderer)
    }

    private fun setUpGarbageListeners() {
        EventOrchestrator.subscribe<GameEvent.LineCleared, Int>(
            { totalLines ->
                if (totalLines != null && totalLines > 0) {
                    garbageProcessor.sendGarbage(totalLines, "all")
                }
            },
            { event -> if (event.gameId == PLAYER_GAME_ID) event.linesCleared.size else null })

    }
}
