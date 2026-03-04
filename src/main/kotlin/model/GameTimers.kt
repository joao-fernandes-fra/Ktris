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
)