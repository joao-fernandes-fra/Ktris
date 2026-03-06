package demo.model

enum class MessagePriority(val level: Int) {
    LOW(0), MEDIUM(1), HIGH(2)
}

data class GameMessage(
    var text: String,
    val priority: MessagePriority,
    val timestamp: Long = System.currentTimeMillis()
){
    override fun toString(): String {
        return "$text:$priority"
    }
}