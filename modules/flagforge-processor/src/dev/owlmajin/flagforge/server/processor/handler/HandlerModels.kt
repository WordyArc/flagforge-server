package dev.owlmajin.flagforge.server.processor.handler

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.CommandPayload
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.EventPayload
import kotlin.reflect.KClass

data class CommandContext<S : Any>(
    val currentState: S?,
)

interface CommandMessageHandler<P : CommandPayload, S : Any> {
    val payloadType: KClass<out P>
    val stateType: KClass<out S>?

    fun isMessageValid(message: CommandMessage<P>, context: CommandContext<S>): Boolean = true

    fun handleMessage(message: CommandMessage<P>, context: CommandContext<S>): MessageHandlingResult.Command
}

data class EventContext<S : Any>(
    val currentState: S?,
)

interface EventMessageHandler<P : EventPayload, S : Any> {
    val payloadType: KClass<out P>
    val stateType: KClass<out S>?

    fun isMessageValid(message: EventMessage<P>, context: EventContext<S>): Boolean = true

    fun handleMessage(message: EventMessage<P>, context: EventContext<S>): MessageHandlingResult.Event
}

sealed interface MessageHandlingResult {

    sealed interface Command : MessageHandlingResult

    data class CommandApplied<E : EventPayload>(
        val event: EventMessage<E>,
    ) : Command

    data class CommandRejected<E : EventPayload>(
        val event: EventMessage<E>,
    ) : Command

    data object CommandIgnored : Command

    sealed interface Event : MessageHandlingResult

    data class EventApplied<S>(
        val newState: S?,
    ) : Event

    data object EventIgnored : Event
}
