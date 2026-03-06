package engine.util

import engine.model.GlobalGameSettings
import engine.model.PlayerSettings

object GameSettingsProvider {
    fun normal() = PlayerSettings() to GlobalGameSettings()

    fun expert() = PlayerSettings(
        dasDelay = 133f,
        arrDelay = 16f,
        lockDelay = 350f,
    ) to GlobalGameSettings(gravityBase = 500f)

    fun pro() = PlayerSettings(
        dasDelay = 110f,
        arrDelay = 0f,
        lockDelay = 200f,
        entryDelay = 0f
    ) to GlobalGameSettings(gravityBase = 150f)
}