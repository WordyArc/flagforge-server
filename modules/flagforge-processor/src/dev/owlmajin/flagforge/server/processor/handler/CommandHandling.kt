package dev.owlmajin.flagforge.server.processor.handler

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.CommandPayload
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.EventPayload
import dev.owlmajin.flagforge.server.model.Message
import kotlin.reflect.KClass

/**
 * Represents context available for command handling.
 * Currently contains only the latest aggregate state, but kept as a dedicated type for future extensions.
 */
data class CommandContext<S : Any>(
    val currentState: S?,
)

interface CommandMessageHandler<P : CommandPayload, S : Any> :
    AbstractMessageHandler<CommandMessage<P>, CommandContext<S>, MessageHandlingResult.Command> {

    override val messageType: KClass<out CommandMessage<P>>
        get() = commandMessageClass as KClass<out CommandMessage<P>>

    val payloadType: KClass<out P>
    val stateType: KClass<out S>?

    override fun isMessageValid(message: CommandMessage<P>, context: CommandContext<S>): Boolean = true

    override fun handleMessage(message: CommandMessage<P>, context: CommandContext<S>): MessageHandlingResult.Command

    companion object {
        @Suppress("UNCHECKED_CAST")
        private val commandMessageClass = Message::class as KClass<out CommandMessage<CommandPayload>>
    }
}

data class EventContext<S : Any>(
    val currentState: S?,
)

interface EventMessageHandler<P : EventPayload, S : Any> :
    AbstractMessageHandler<EventMessage<P>, EventContext<S>, MessageHandlingResult.Event> {

    override val messageType: KClass<out EventMessage<P>>
        get() = eventMessageClass as KClass<out EventMessage<P>>

    val payloadType: KClass<out P>
    val stateType: KClass<out S>?

    override fun isMessageValid(message: EventMessage<P>, context: EventContext<S>): Boolean = true

    override fun handleMessage(message: EventMessage<P>, context: EventContext<S>): MessageHandlingResult.Event

    companion object {
        @Suppress("UNCHECKED_CAST")
        private val eventMessageClass = Message::class as KClass<out EventMessage<EventPayload>>
    }
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
