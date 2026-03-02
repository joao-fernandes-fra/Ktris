package demo

import controller.defaults.BaseTetris
import controller.defaults.ModernGuidelineRules
import controller.defaults.ScoreRegistry
import controller.defaults.TimeManager
import model.AppLog
import model.GameConfig
import model.GameEventBus
import model.GameGoal
import model.defaults.MultiBagRandomizer
import model.defaults.ProceduralPiece
import model.defaults.Tetromino
import javax.swing.JFrame
import javax.swing.Timer
import javax.swing.WindowConstants

fun main() {
    AppLog.minLevel = AppLog.Level.INFO
    val frame = JFrame("Ktris")
    val eventBus = GameEventBus()
    // this is the object that would handle a menu, it has default settings, but it's all mutable and should be updated before starting the game
    val gameConfig = GameConfig(goalType = GameGoal.LINES, goalValue = 20)
    val timeManager = TimeManager(gameConfig)
    val game = BaseTetris(
        settings = gameConfig,
        bagManager = MultiBagRandomizer(Tetromino.values),
        eventBus = eventBus,
        timeManager = timeManager,
    )

    val scoreRegistry = ScoreRegistry(ModernGuidelineRules(), eventBus)
    val renderer = SwingRenderer<ProceduralPiece>(scoreRegistry, 10, 20, eventBus)
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