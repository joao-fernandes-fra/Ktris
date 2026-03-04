package demo

import controller.TetrisEngine
import controller.defaults.BaseTetris
import controller.defaults.ModernGuidelineRules
import controller.defaults.ScoreRegistry
import controller.defaults.TimeManager
import model.AppLog
import model.Command
import model.GameGoal
import model.defaults.ProceduralPiece
import model.defaults.Tetromino
import model.events.EventHandler
import model.events.GameEvent
import model.events.GameEvent.GarbageSent
import model.events.InputEvent
import model.events.MultiBagRandomizer
import util.GameSettingsProvider
import javax.swing.JFrame
import javax.swing.WindowConstants
import kotlin.random.Random

private const val GARBAGE_BLOCK_ID = -99


fun main(args: Array<String>) {
    GameEvent.registerEvents()
    InputEvent.registerEvents()
    AppLog.minLevel = AppLog.Level.DEBUG

    val baseSettings = when {
        args.contains("expert") -> GameSettingsProvider.expert()
        args.contains("pro") -> GameSettingsProvider.pro()
        else -> GameSettingsProvider.normal()
    }
    val isCheeseGame = args.contains("cheese")

    val gameSettings = baseSettings.copy(
        goalType = GameGoal.TIME,
        goalValue = 2 * 60f
    )

    val frame = JFrame("Ktris - ${if (args.isEmpty()) "Normal" else args[0].uppercase()}")
    val timeManager = TimeManager(gameSettings)

    val game = BaseTetris(
        settings = gameSettings,
        bagManager = MultiBagRandomizer(Tetromino.values),
        timeManager = timeManager,
    )

    EventHandler.subscribeToEvent<InputEvent.CommandInput> { event ->
        if (event.command == Command.RESET && isCheeseGame) {
            startCheeseRows()
        }
    }

    if (isCheeseGame) {
        setupCheeseGame(game)
    }

    val scoreRegistry = ScoreRegistry(ModernGuidelineRules())
    val renderer = SwingRenderer<ProceduralPiece>(scoreRegistry, game)
    val inputHandler = SwingInputHandler(timeManager)

    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.add(renderer)
    frame.addKeyListener(inputHandler)
    frame.pack()
    frame.isVisible = true

    game.start(renderer)
}

private fun setupCheeseGame(game: TetrisEngine<*>) {
    EventHandler.subscribeToEvent<GarbageSent> {
        game.processGarbage(it.lines, GARBAGE_BLOCK_ID)
    }

    startCheeseRows()

    EventHandler.subscribeToEvent<GameEvent.LineCleared> {
        if (it.linesCleared > 0)
            EventHandler.publish(GarbageSent.topic, GarbageSent(it.linesCleared * (it.spinType.ordinal + 1)))
    }
}

private fun startCheeseRows() {
    val parts = mutableListOf<Int>()
    var remaining = 5
    val minPart = 1
    while (remaining > 0) {
        val upperLimit = minOf(remaining, 2)

        val part = Random.nextInt(minPart, upperLimit + 1)

        parts.add(part)
        remaining -= part
    }

    parts.forEach {
        EventHandler.publish(GarbageSent.topic, GarbageSent(it))
    }
}