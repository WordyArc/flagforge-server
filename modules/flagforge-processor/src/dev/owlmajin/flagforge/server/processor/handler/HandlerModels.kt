package dev.owlmajin.flagforge.server.processor.handler

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.CommandPayload
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.EventPayload
import kotlin.reflect.KClass

data class CommandContext<S : Any>(
    val currentState: S?,
)


data class EventContext<S : Any>(
    val currentState: S?,
)

interface CommandMessageHandler<P : CommandPayload, S : Any> {
    val payloadType: KClass<out P>
    val stateType: KClass<out S>?

    fun isMessageValid(message: CommandMessage<P>, context: CommandContext<S>): Boolean = true

    fun handleMessage(message: CommandMessage<P>, context: CommandContext<S>): CommandResult
}

interface EventMessageHandler<P : EventPayload, S : Any> {
    val payloadType: KClass<out P>
    val stateType: KClass<out S>?

    fun isMessageValid(message: EventMessage<P>, context: EventContext<S>): Boolean = true

    fun handleMessage(message: EventMessage<P>, context: EventContext<S>): EventResult
}

sealed interface CommandResult {
    val event: EventMessage<*>?

    data class Applied(
        override val event: EventMessage<*>,
    ) : CommandResult

    data class Rejected(
        override val event: EventMessage<*>,
    ) : CommandResult

    data object Ignored : CommandResult {
        override val event: EventMessage<*>? = null
    }
}

sealed interface EventResult {

    data class Applied<S: Any?>(
        val newState: S?,
    ) : EventResult

    data object Ignored : EventResult
}
