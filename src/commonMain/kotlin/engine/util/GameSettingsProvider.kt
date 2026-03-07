package engine.util

import engine.model.GameSettings
import engine.model.PlayerSettings

object GameSettingsProvider {
    fun normal() = PlayerSettings() to GameSettings()

    fun expert() = PlayerSettings(
        dasDelay = 133.0,
        arrDelay = 16.0,
        lockDelay = 350.0,
    ) to GameSettings(gravityBase = 500.0)

    fun pro() = PlayerSettings(
        dasDelay = 110.0,
        arrDelay = 0.0,
        lockDelay = 200.0,
        entryDelay = 0.0
    ) to GameSettings(gravityBase = 150.0)
}