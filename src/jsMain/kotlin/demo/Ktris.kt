package demo

import controller.defaults.BaseTetris
import controller.defaults.ModernGuidelineRules
import controller.defaults.ScoreRegistry
import controller.defaults.TimeManager
import kotlinx.browser.document
import kotlinx.browser.window
import model.AppLog
import model.Command
import model.GameGoal
import model.Level
import model.defaults.ProceduralPiece
import model.defaults.Tetromino
import model.events.EventHandler
import model.events.GameEvent
import model.events.GameEvent.GarbageSent
import model.events.InputEvent
import model.events.MultiBagRandomizer
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import util.GameSettingsProvider
import kotlin.random.Random

private const val GARBAGE_BLOCK_ID = -99

fun main() {
    GameEvent.registerEvents()
    InputEvent.registerEvents()
    AppLog.minLevel = Level.DEBUG

    val params = window.location.search.removePrefix("?").split("&").filter { it.isNotEmpty() }
    val baseSettings = when {
        "expert" in params -> GameSettingsProvider.expert()
        "pro" in params -> GameSettingsProvider.pro()
        else -> GameSettingsProvider.normal()
    }
    val isCheeseGame = "cheese" in params

    val gameSettings = if ("4way" in params)
        baseSettings.copy(
            goalType = GameGoal.TIME,
            goalValue = 2 * 60f,
            boardCols = 4
        )
    else baseSettings.copy(
        goalType = GameGoal.TIME,
        goalValue = 2 * 60f
    )

    val canvas = document.getElementById("gameCanvas") as HTMLCanvasElement
    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D

    val timeManager = TimeManager(gameSettings)
    val game = BaseTetris(
        settings = gameSettings,
        bagManager = MultiBagRandomizer(Tetromino.values),
        timeManager = timeManager
    )

    if (isCheeseGame) {
        setupCheeseGame(game)
        EventHandler.subscribeToEvent<InputEvent.CommandInput> { event ->
            if (event.command == Command.RESET) {
                startCheeseRows()
            }
        }
    }

    val scoreRegistry = ScoreRegistry(ModernGuidelineRules())
    val renderer = WebRenderer<ProceduralPiece>(scoreRegistry, game, ctx)
    val inputHandler = WebInputHandler(timeManager)

    game.start(renderer)

    fun gameLoop() {
        renderer.render(game.gameStateSnapshot())
        window.requestAnimationFrame { gameLoop() }
    }
    gameLoop()
}

private fun setupCheeseGame(game: BaseTetris<*>) {
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