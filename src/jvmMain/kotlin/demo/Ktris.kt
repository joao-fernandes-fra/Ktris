package demo

import demo.controller.SwingInputHandler
import demo.model.SwingTetris
import demo.model.SwingTetris.Companion.PLAYER_GAME_ID
import demo.utils.GameSettingsProvider
import demo.view.SwingRenderer
import demo.utils.GameRegistry
import engine.model.BoardConfig
import engine.model.GameGoal
import engine.model.ObjectiveConfig
import engine.util.Logger
import engine.model.defaults.Tetromino
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.uuid.ExperimentalUuidApi


@OptIn(ExperimentalUuidApi::class)
fun main(args: Array<String>) {
    Logger.minLevel = Logger.Level.DEBUG

    val (playerSettings, gameSettings) = when {
        args.contains("expert") -> GameSettingsProvider.expert()
        args.contains("pro") -> GameSettingsProvider.pro()
        else -> GameSettingsProvider.normal()
    }

    val resolvedSettings = if (args.contains("4way"))
        gameSettings.copy(
            board = BoardConfig(cols = 4),
            objective = ObjectiveConfig(goalType = GameGoal.TIME, goalValue = 120.0)
        )
    else
        gameSettings.copy(objective = ObjectiveConfig(goalType = GameGoal.TIME, goalValue = 120.0))

    val isCheeseGame = args.contains("versus")
    val enemyApm = if (isCheeseGame) when {
        args.contains("expert") -> 30
        args.contains("pro") -> 60
        else -> 10
    } else null

    val context = GameRegistry.getDefaultContext(resolvedSettings, playerSettings, Tetromino.pieces, PLAYER_GAME_ID)
    val gameScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val engine = SwingTetris(context)
    val renderer = SwingRenderer(context)
    val inputHandler = SwingInputHandler(engine)

    engine.initialize(isCheeseGame, enemyApm, gameScope)

    SwingUtilities.invokeLater {
        val frame = JFrame("Ktris - ${if (args.isEmpty()) "Normal" else args[0].uppercase()}")
        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        frame.add(renderer)
        frame.addKeyListener(inputHandler)
        frame.pack()
        frame.isVisible = true

        engine.start(renderer)
    }
}