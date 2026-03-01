package demo

import model.AppLog
import model.BaseTetris
import model.Tetromino
import model.GameConfig
import model.GameEventBus
import model.GameGoal
import model.ProceduralPiece
import model.ModernGuidelineRules
import model.MultiBagRandomizer
import model.ScoreRegistry
import javax.swing.JFrame
import javax.swing.Timer
import javax.swing.WindowConstants

fun main() {
    AppLog.minLevel = AppLog.Level.INFO
    val frame = JFrame("Ktris")
    val eventBus = GameEventBus()

    // this is the object that would handle a menu, it has default settings, but it's all mutable and should be updated before starting the game
    val gameConfig = GameConfig(goalType = GameGoal.LINES, goalValue = 40)
    val scoreRegistry = ScoreRegistry(ModernGuidelineRules(), eventBus)
    val renderer = SwingRenderer<ProceduralPiece>(scoreRegistry, 10, 20, eventBus)
    val inputHandler = SwingInputHandler(eventBus)

    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.add(renderer)
    frame.addKeyListener(inputHandler)
    frame.pack()
    frame.isVisible = true

    val game = BaseTetris(
        gameConfig,
        MultiBagRandomizer(Tetromino.values),
        eventBus,
    )

    var lastTime = System.nanoTime()

    Timer(16) {
        val currentTime = System.nanoTime()
        val dt = (currentTime - lastTime) / 1_000_000f
        lastTime = currentTime

        val frameTime = if (dt > 100f) 100f else dt

        game.deltaTime = frameTime
        game.update()

        renderer.render(game.gameStateSnapshot())
    }.start()
}