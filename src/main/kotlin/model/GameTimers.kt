package model

data class GameTimers(
    var dropTimer: Float = 1.0f,
    var lockTimer: Float = 1.0f,
    var sessionTimer: Float = 0f,
    var dasTimer: Float = 1.0f,
    var arrTimer: Float = 1.0f,
    var softDropTimer: Float = 1.0f,
    var areTimer: Float = 0.0f,
    var dasFrameCounter: Int = 0
)