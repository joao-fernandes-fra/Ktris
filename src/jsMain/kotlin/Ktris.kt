package web

import controller.defaults.BaseTetris
import controller.defaults.ModernGuidelineRules
import controller.defaults.ScoreRegistry
import controller.defaults.TimeManager
import demo.WebInputHandler
import demo.WebRenderer
import kotlinx.browser.document
import kotlinx.browser.window
import model.AppLog
import model.GameTimers
import model.LogLevel
import model.defaults.ProceduralPiece
import model.defaults.Tetromino
import model.events.GameEvent
import model.events.InputEvent
import model.events.MultiBagRandomizer
import model.info
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import util.GameSettingsProvider


fun main() {
    window.addEventListener("DOMContentLoaded", {
        startApplication()
    })
}

fun startApplication() {
    GameEvent.registerEvents()
    InputEvent.registerEvents()
    AppLog.minLogLevel = LogLevel.DEBUG

    val gameSettings = GameSettingsProvider.normal()

    val canvasElement = document.getElementById("gameCanvas")
    val canvas = canvasElement as? HTMLCanvasElement
        ?: throw IllegalStateException("Could not find element with id 'gameCanvas' or it is not a <canvas> element.")

    val ctx = canvas.getContext("2d") as? CanvasRenderingContext2D
        ?: throw IllegalStateException("Could not retrieve 2D rendering context.")

    val timeManager = TimeManager(gameSettings)
    val gameTimers = GameTimers()
    val game = BaseTetris(
        gameTimers = gameTimers,
        settings = gameSettings,
        bagManager = MultiBagRandomizer(Tetromino.values),
        timeManager = timeManager
    )

    val scoreRegistry = ScoreRegistry(ModernGuidelineRules())
    val renderer = WebRenderer<ProceduralPiece>(scoreRegistry, gameTimers, ctx)
    val inputHandler = WebInputHandler(scoreRegistry)

    var lastTimestamp = 0.0

    fun gameLoop(timestamp: Double) {
        if (lastTimestamp == 0.0) {
            lastTimestamp = timestamp
            window.requestAnimationFrame(::gameLoop)
            return
        }

        val deltaMs = (timestamp - lastTimestamp).toFloat()
        lastTimestamp = timestamp

        game.update(deltaMs)

        renderer.render(game.gameStateSnapshot())

        window.requestAnimationFrame(::gameLoop)
    }

    window.requestAnimationFrame(::gameLoop)
}