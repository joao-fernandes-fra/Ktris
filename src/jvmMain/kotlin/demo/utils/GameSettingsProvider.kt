package demo.utils

import engine.model.GameplayConfig
import engine.model.GravityConfig
import engine.model.HandlingConfig
import engine.model.MatchConfig
import engine.model.PlayerConfig

object GameSettingsProvider {
    fun normal() = PlayerConfig() to MatchConfig()

    fun expert() = PlayerConfig(
        HandlingConfig(
            dasDelay = 133.0,
            arrDelay = 16.0,
        )
    ) to MatchConfig(gravity = GravityConfig(lockDelay = 350.0, gravityBase = 500.0))

    fun pro() = PlayerConfig(
        HandlingConfig(
            dasDelay = 133.0,
            arrDelay = 16.0,
        )
    ) to MatchConfig(
        gravity = GravityConfig(lockDelay = 200.0, gravityBase = 150.0),
        gameplay = GameplayConfig(entryDelay = 0.0)
    )
}