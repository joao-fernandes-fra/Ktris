package model

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

class GameEventBus {
    val subscribers = ConcurrentHashMap<KClass<*>, CopyOnWriteArrayList<(Any) -> Unit>>()

    inline fun <reified T : Any> subscribe(noinline handler: (T) -> Unit) {
        val type = T::class
        val handlers = subscribers.getOrPut(type) { CopyOnWriteArrayList() }
        @Suppress("UNCHECKED_CAST")
        handlers.add(handler as (Any) -> Unit)
    }

    fun <T : Any> post(event: T) {
        subscribers[event::class]?.forEach { it(event) }

        subscribers.forEach { (kClass, callbackList) ->
            if (kClass.isInstance(event)) {
                callbackList.forEach { it(event) }
            }
        }
    }

    inline fun <reified T : Any> unsubscribe() {
        val targetClass = T::class.java

        val iterator = subscribers.keys.iterator()
        while (iterator.hasNext()) {
            val registeredClass = iterator.next().java

            if (targetClass.isAssignableFrom(registeredClass)) {
                iterator.remove()
            }
        }
    }
}