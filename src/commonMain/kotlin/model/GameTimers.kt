package model

data class GameTimers(
    var dropTimer: Float = 0f,
    var lockTimer: Float = 0f,
    var sessionTimer: Float = 0f,
    var dasTimer: Float = 0f,
    var arrTimer: Float = 0f,
    var softDropTimer: Float = 0f,
    var areTimer: Float = 0f,
    var dasFrameCounter: Int = 0
) : Resetable {
    override fun reset() {
        dropTimer = 0f
        lockTimer = 0f
        sessionTimer = 0f
        dasTimer = 0f
        arrTimer = 0f
        softDropTimer = 0f
        areTimer = 0f
        dasFrameCounter = 0
    }
}