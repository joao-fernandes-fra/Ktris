package engine.model.defaults

import kotlinx.serialization.Serializable

import engine.model.MoveType

@Serializable
sealed class TetrisMoveType : MoveType {
    abstract override val isSpecial: Boolean
    abstract override val displayName: String
    override val id: String get() = this::class.simpleName ?: error("No simple name")

    @Serializable object NONE : TetrisMoveType() {
        override val isSpecial = false
        override val displayName = ""
    }

    @Serializable object SINGLE : TetrisMoveType() {
        override val isSpecial = false
        override val displayName = "Single"
    }

    @Serializable object DOUBLE : TetrisMoveType() {
        override val isSpecial = false
        override val displayName = "Double"
    }

    @Serializable object TRIPLE : TetrisMoveType() {
        override val isSpecial = false
        override val displayName = "Triple"
    }

    @Serializable object TETRIS : TetrisMoveType() {
        override val isSpecial = true
        override val displayName = "Tetris"
    }

    @Serializable object T_SPIN_MINI_SINGLE : TetrisMoveType() {
        override val isSpecial = true
        override val displayName = "T-Spin Mini Single"
    }

    @Serializable object T_SPIN_MINI_DOUBLE : TetrisMoveType() {
        override val isSpecial = true
        override val displayName = "T-Spin Mini Double"
    }

    @Serializable object T_SPIN_SINGLE : TetrisMoveType() {
        override val isSpecial = true
        override val displayName = "T-Spin Single"
    }

    @Serializable object T_SPIN_DOUBLE : TetrisMoveType() {
        override val isSpecial = true
        override val displayName = "T-Spin Double"
    }

    @Serializable object T_SPIN_TRIPLE : TetrisMoveType() {
        override val isSpecial = true
        override val displayName = "T-Spin Triple"
    }
}