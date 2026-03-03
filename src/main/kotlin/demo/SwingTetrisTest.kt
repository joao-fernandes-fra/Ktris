package demo

import controller.TetrisEngine
import controller.defaults.BaseTetris
import controller.defaults.ModernGuidelineRules
import controller.defaults.ScoreRegistry
import controller.defaults.TimeManager
import model.AppLog
import model.GameEvent
import model.GameEventBus
import model.GameGoal
import model.GameSettings
import model.defaults.MultiBagRandomizer
import model.defaults.ProceduralPiece
import model.defaults.Tetromino
import javax.swing.JFrame
import javax.swing.Timer
import javax.swing.WindowConstants
import kotlin.random.Random

private const val GARBAGE_BLOCK_ID = -99

fun main(args: Array<String>) {
    AppLog.minLevel = AppLog.Level.DEBUG
    val frame = JFrame("Ktris")
    val eventBus = GameEventBus()
    // this is the object that would handle a menu, it has default settings, but it's all mutable and should be updated before starting the game
    val gameSettings = GameSettings(goalType = GameGoal.TIME, goalValue = 2 * 60)
    val timeManager = TimeManager(gameSettings)
    val game = BaseTetris(
        settings = gameSettings,
        bagManager = MultiBagRandomizer(Tetromino.values),
        eventBus = eventBus,
        timeManager = timeManager,
    )

    if (args.contains("cheese")) {
        setupCheeseGame(eventBus, game)
    }

    val scoreRegistry = ScoreRegistry(ModernGuidelineRules(), eventBus)
    val renderer = SwingRenderer<ProceduralPiece>(scoreRegistry, game, eventBus)
    val inputHandler = SwingInputHandler(eventBus, timeManager)

    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.add(renderer)
    frame.addKeyListener(inputHandler)
    frame.pack()
    frame.isVisible = true


    var lastTime = System.nanoTime()

    Timer(16) {
        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastTime) / 1_000_000f
        lastTime = currentTime

        val frameTime = if (deltaTime > 100f) 100f else deltaTime

        game.deltaTime = frameTime
        game.update()

        renderer.render(game.gameStateSnapshot())
    }.start()
}

private fun setupCheeseGame(eventBus: GameEventBus, game: TetrisEngine<*>) {
    val parts = mutableListOf<Int>()
    var remaining = 10
    val minPart = 1
    while (remaining > 0) {
        val upperLimit = minOf(remaining, 4)

        val part = Random.nextInt(minPart, upperLimit + 1)

        parts.add(part)
        remaining -= part
    }

    eventBus.subscribe<GameEvent.GarbageSent> {
        game.processGarbage(it.lines, GARBAGE_BLOCK_ID)
    }

    eventBus.subscribe<GameEvent.LineCleared> {
        if (it.linesCleared > 0)
            eventBus.post(GameEvent.GarbageSent(it.linesCleared * it.spinType.ordinal + 1))
    }

    parts.forEach {
        eventBus.post(GameEvent.GarbageSent(it))
    }
}