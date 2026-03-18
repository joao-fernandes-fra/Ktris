package engine.model

import engine.controller.LevelHandler
import engine.controller.defaults.gravity.DefaultLevelHandler

data class BoardConfig(
    val rows: Int = 20,
    val cols: Int = 10,
    val bufferZone: Int = 4,
)

data class HandlingConfig(
    val dasDelay: Double = 167.0,
    val arrDelay: Double = 33.0,
    val softDropFactor: Double = 35.0,
    val dcdDelay: Double = 0.0,
    val dasCutOnRotation: Boolean = false,
    val dasPreservation: DasPreservation = DasPreservation.FULL,
    val cancelDasOnDirectionChange: Boolean = true,
    val preventAccidentalHardDrop: Boolean = true,
    val irsMode: BufferMode = BufferMode.HOLD,
    val ihsMode: BufferMode = BufferMode.HOLD,
)

data class GravityConfig(
    val levelHandler: LevelHandler = DefaultLevelHandler(),
    val lockDelay: Double = 500.0,
    val maxLockResets: Int = 15,
    val shouldCollapseOnFreeze: Boolean = true,
)

data class ObjectiveConfig(
    val goalType: GameGoal = GameGoal.NONE,
    val goalValue: Double = 0.0,
    val toppingOutIsOk: Boolean = false,
)

data class GarbageConfig(
    val garbageBlockId: Int = -99,
    val messiness: Int = 100,
    val cap: Int = 0,
)

data class GameplayConfig(
    val spinDetection: SpinMode = SpinMode.ALL_MINI_PLUS,
    val isHoldEnabled: Boolean = true,
    val isGhostEnabled: Boolean = true,
    val is180Enabled: Boolean = false,
    val useHardDrop: Boolean = true,
    val infiniteMovement: Boolean = false,
    val infiniteHold: Boolean = false,
    val previewSize: Int = 5,
    val entryDelay: Double = 0.0,
    val lineClearDelay: Double = 0.0,
)


data class MatchConfig(
    val board: BoardConfig = BoardConfig(),
    val gravity: GravityConfig = GravityConfig(),
    val gameplay: GameplayConfig = GameplayConfig(),
    val garbage: GarbageConfig = GarbageConfig(),
    val objective: ObjectiveConfig = ObjectiveConfig(),
)

data class PlayerConfig(
    val handling: HandlingConfig = HandlingConfig(),
)

enum class SpinMode {
    NONE,               // no spins ever
    PIECE_DEFINED,      // eligible pieces only, standard detection
    PIECE_DEFINED_PLUS, // eligible pieces only, immobile also qualifies
    ALL,                // all pieces, standard detection
    ALL_PLUS,           // all pieces, immobile also qualifies
    ALL_MINI,           // eligible = standard, others = always mini
    ALL_MINI_PLUS,      // eligible = standard + immobile, others = always mini
    STUPID,             // everything is always FULL
}

enum class BufferMode { OFF, HOLD, TAP }
enum class GameGoal { LINES, TIME, NONE }
enum class DasPreservation { FULL, CHARGE_ONLY, RESET }