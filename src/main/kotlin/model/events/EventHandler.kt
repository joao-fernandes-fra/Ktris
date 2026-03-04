package model.events

import controller.MoveType
import controller.defaults.TetrisMoveType
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer
import model.AppLog
import model.Piece
import model.defaults.ProceduralIPiece
import model.defaults.ProceduralPiece
import model.defaults.ProceduralTPiece
import model.events.GameEvent.BackToBackTrigger
import model.events.GameEvent.ComboTriggered
import model.events.GameEvent.FreezeLineClear
import model.events.GameEvent.GameOver
import model.events.GameEvent.GarbageReceived
import model.events.GameEvent.GarbageSent
import model.events.GameEvent.HardDrop
import model.events.GameEvent.LevelUp
import model.events.GameEvent.LineCleared
import model.events.GameEvent.NewPiece
import model.events.GameEvent.PieceHeld
import model.events.GameEvent.PieceLocked
import model.events.GameEvent.PieceRotated
import model.events.GameEvent.ScoreUpdated
import model.events.GameEvent.SfxTrigger
import model.events.GameEvent.SoftDrop
import model.events.GameEvent.SpinDetected
import model.events.InputEvent.CommandInput
import model.events.InputEvent.DirectionMoveEnd
import model.events.InputEvent.DirectionMoveStart
import model.events.InputEvent.DropInput
import model.events.InputEvent.FreezeTime
import model.events.InputEvent.RotationInputRelease
import model.events.InputEvent.RotationInputStart
import model.events.InputEvent.SlowDownTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass


object EventHandler {
    val executor: ExecutorService = Executors.newFixedThreadPool(2)
    val listeners = ConcurrentHashMap<String, CopyOnWriteArrayList<(String) -> Unit>>()
    private val topicMap = mutableMapOf<KClass<out Event>, String>()

    private val jsonConfig = AtomicReference(
        Json {
            serializersModule = SerializersModule {
                polymorphic(Piece::class) {
                    subclass(ProceduralPiece::class)
                    subclass(ProceduralIPiece::class)
                    subclass(ProceduralTPiece::class)
                }
                polymorphic(Event::class) {
                    subclass(GameEvent::class)
                    subclass(GameOver::class)
                    subclass(NewPiece::class)
                    subclass(PieceHeld::class)
                    subclass(PieceRotated::class)
                    subclass(HardDrop::class)
                    subclass(SoftDrop::class)
                    subclass(PieceLocked::class)
                    subclass(LineCleared::class)
                    subclass(FreezeLineClear::class)
                    subclass(ScoreUpdated::class)
                    subclass(SpinDetected::class)
                    subclass(LevelUp::class)
                    subclass(ComboTriggered::class)
                    subclass(BackToBackTrigger::class)
                    subclass(SfxTrigger::class)
                    subclass(GarbageSent::class)
                    subclass(GarbageReceived::class)
                    subclass(InputEvent::class)
                    subclass(SlowDownTime::class)
                    subclass(FreezeTime::class)
                    subclass(DirectionMoveStart::class)
                    subclass(DirectionMoveEnd::class)
                    subclass(DropInput::class)
                    subclass(CommandInput::class)
                    subclass(RotationInputStart::class)
                    subclass(RotationInputRelease::class)
                }
            }
            ignoreUnknownKeys = true
        }
    )

    val json: Json get() = jsonConfig.get()

    fun withModule(newModule: SerializersModule): EventHandler {
        val currentModule = jsonConfig.get().serializersModule
        val combinedModule = currentModule + newModule

        jsonConfig.set(
            Json {
                serializersModule = combinedModule
                ignoreUnknownKeys = true
            }
        )
        return this
    }

    fun subscribe(topic: String, callback: (String) -> Unit) {
        listeners.computeIfAbsent(topic) { CopyOnWriteArrayList() }.add(callback)
    }

    inline fun <reified T : Event> publish(topic: String, payload: T) {
        val jsonString = json.encodeToString(serializer(), payload)
        register(T::class, topic)
        dispatchInternal(topic, jsonString)
    }

    inline fun <reified T : Event> subscribeToEvent(crossinline callback: (T) -> Unit) {
        val topic = getTopic(T::class)

        subscribe(topic) { rawData ->
            val event = Json.decodeFromString<T>(rawData)
            callback(event)
        }
    }

    inline fun <reified T : Event> subscribeToEvent(clazz: KClass<out Event>, crossinline callback: (T) -> Unit) {
        val topic = getTopic(clazz)

        subscribe(topic) { rawData ->
            val event = json.decodeFromString<T>(rawData)
            callback(event)
        }
    }

    fun register(clazz: KClass<out Event>, topic: String) {
        topicMap[clazz] = topic
    }

    fun getTopic(clazz: KClass<out Event>): String =
        topicMap[clazz] ?: throw IllegalArgumentException("Topic not found for ${clazz.simpleName}")

    fun dispatchInternal(topic: String, jsonString: String) {
        listeners[topic]?.forEach { callback ->
            executor.submit {
                try {
                    callback(jsonString)
                } catch (e: Exception) {
                    AppLog.error { "Error in listener: [${e.message}] at topic: [$topic]" }
                }
            }
        }
    }
}