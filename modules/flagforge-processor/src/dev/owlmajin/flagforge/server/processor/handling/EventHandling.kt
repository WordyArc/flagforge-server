package dev.owlmajin.flagforge.server.processor.handling

import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.EventPayload
import kotlin.reflect.KClass

data class EventContext<S : Any>(
    val currentState: S?,
)

sealed interface EventResult {

    data class Applied<S: Any?>(
        val previousState: S?,
        val newState: S?,
    ) : EventResult

    data object Ignored : EventResult
}

interface EventHandler<P : EventPayload, S : Any> {
    val payloadType: KClass<out P>
    val stateType: KClass<out S>?

    fun isMessageValid(message: EventMessage<P>, context: EventContext<S>): Boolean = true

    fun handleMessage(message: EventMessage<P>, context: EventContext<S>): EventResult
}