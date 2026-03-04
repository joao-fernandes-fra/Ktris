package util

import model.GameSettings

object GameSettingsProvider {
    fun normal() = GameSettings()

    fun expert() = GameSettings(
        dasDelay = 133f,
        arrDelay = 16f,
        lockDelay = 350f,
        gravityBase = 500f
    )

    fun pro() = GameSettings(
        dasDelay = 110f,
        arrDelay = 0f,
        lockDelay = 200f,
        gravityBase = 150f,
        entryDelay = 0f
    )
}