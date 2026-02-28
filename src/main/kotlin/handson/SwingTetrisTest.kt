package handson

import model.AppLog
import model.GameConfig
import model.GameEvent
import model.GameEventBus
import model.GameGoal
import model.ModernGuidelineRules
import model.MultiBagRandomizer
import model.ScoreRegistry
import model.GuidelinesComplianceGame
import javax.swing.JFrame
import javax.swing.Timer
import javax.swing.WindowConstants

fun main() {
    val frame = JFrame("Tetris")
    val eventBus = GameEventBus()
    val scoreRegistry = ScoreRegistry(ModernGuidelineRules(), eventBus)
    val renderer = SwingRenderer(scoreRegistry, 10, 20)
    val inputHandler = SwingInputHandler(eventBus)

    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.add(renderer)
    frame.addKeyListener(inputHandler)
    frame.pack()
    frame.isVisible = true

    val fixedStep = 16.0f
    val game = GuidelinesComplianceGame(
        GameConfig(goalType = GameGoal.LINES, goalValue = 40, arrDelay = 0f),
        MultiBagRandomizer(),
        eventBus,
        null,
        fixedStep
    )

    eventBus.subscribe<GameEvent> {
        AppLog.info(msg = "Game Event: $it")
    }
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