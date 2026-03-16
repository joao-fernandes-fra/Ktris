package engine.model

data class PieceTimers(
    var dropTimer: Double = 0.0,
    var lockTimer: Double = 0.0,
    var dasTimer: Double = 0.0,
    var arrTimer: Double = 0.0,
    var softDropTimer: Double = 0.0,
    var dasFrameCounter: Int = 0,
    var dcdTimer: Double = 0.0,
) : Resetable {
    override fun reset() {
        dropTimer = 0.0
        lockTimer = 0.0
        dasTimer = 0.0
        arrTimer = 0.0
        softDropTimer = 0.0
        dasFrameCounter = 0
        dcdTimer = 0.0
    }
}

data class EngineTimers(
    var sessionTimer: Double = 0.0,
    var areTimer: Double = 0.0,
    var lineClearTimer: Double = 0.0,
) : Resetable {
    val sessionTimeSeconds get() = sessionTimer / 1000.0
    override fun reset() {
        sessionTimer = 0.0
        areTimer = 0.0
        lineClearTimer = 0.0
    }
}