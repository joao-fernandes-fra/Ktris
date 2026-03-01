package model

class TimeManager(private val settings: GameConfig) {
    var mode: TimeMode = TimeMode.NORMAL
        private set

    var onFreezeEnded: (() -> Unit)? = null

    private var modeDuration: Float = 0f

    fun freezeTime(duration: Float) {
        mode = TimeMode.FROZEN
        modeDuration = duration
    }

    fun slowDownTime(duration: Float) {
        mode = TimeMode.SLOWED
        modeDuration = duration
    }

    fun tick(deltaTime: Float): Float {
        if (mode == TimeMode.NORMAL) return deltaTime

        if (mode == TimeMode.SLOWED) {
            return deltaTime * settings.slowDownMultiplier
        }
        
        modeDuration -= deltaTime
        if (modeDuration <= 0) {
            mode = TimeMode.NORMAL
            onFreezeEnded?.invoke()
        }
        
        return 0f
    }
}