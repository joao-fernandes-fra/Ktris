package engine.model.defaults

import engine.model.MoveType
import engine.model.SpinType

sealed class DefaultMoveType : MoveType {
    abstract override val isSpecial: Boolean
    abstract override val displayName: String
    override val id: String get() = this::class.simpleName ?: error("No simple name")

    object NONE : DefaultMoveType() {
        override val isSpecial = false
        override val displayName = ""
    }

    object SINGLE : DefaultMoveType() {
        override val isSpecial = false
        override val displayName = "Single"
    }

    object DOUBLE : DefaultMoveType() {
        override val isSpecial = false
        override val displayName = "Double"
    }

    object TRIPLE : DefaultMoveType() {
        override val isSpecial = false
        override val displayName = "Triple"
    }

    object QUAD : DefaultMoveType() {
        override val isSpecial = true
        override val displayName = "Quad"
    }
}

data class SpinMoveType(
    val pieceName: String,
    val spinType: SpinType,
    val lines: Int
) : MoveType {
    override val isSpecial = true
    override val id = "${pieceName}_${spinType}_$lines"
    override val displayName = buildString {
        append(pieceName)
        append(" Spin")
        if (spinType == SpinType.MINI) append(" Mini")
        when (lines) {
            1 -> append(" Single")
            2 -> append(" Double")
            3 -> append(" Triple")
            4 -> append(" Quad")
            else -> if (lines > 0) append(" $lines Lines")
        }
    }
}