package engine.model

data class GameTimers(
    var dropTimer: Double = 0.0,
    var lockTimer: Double = 0.0,
    var sessionTimer: Double = 0.0,
    var dasTimer: Double = 0.0,
    var arrTimer: Double = 0.0,
    var softDropTimer: Double = 0.0,
    var areTimer: Double = 0.0,
    var dasFrameCounter: Int = 0
) : Resetable {
    val sessionTimeSeconds get() = sessionTimer / 1000.0
    override fun reset() {
        dropTimer = 0.0
        lockTimer = 0.0
        sessionTimer = 0.0
        dasTimer = 0.0
        arrTimer = 0.0
        softDropTimer = 0.0
        areTimer = 0.0
        dasFrameCounter = 0
    }
}