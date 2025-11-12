package dev.owlmajin.flagforge.server.processor

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.CommandPayload
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.EventPayload
import dev.owlmajin.flagforge.server.processor.handler.CommandContext
import dev.owlmajin.flagforge.server.processor.handler.CommandMessageHandler
import dev.owlmajin.flagforge.server.processor.handler.EventContext
import dev.owlmajin.flagforge.server.processor.handler.EventMessageHandler
import dev.owlmajin.flagforge.server.processor.handler.MessageHandlingResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class MessageProcessor(
    commandHandlers: List<CommandMessageHandler<*, *>>, 
    eventHandlers: List<EventMessageHandler<*, *>>, 
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val commandHandlersIndex: List<CommandHandlerEntry> =
        commandHandlers.map { handler ->
            @Suppress("UNCHECKED_CAST")
            CommandHandlerEntry(
                handler = handler,
                payloadType = handler.payloadType as KClass<out CommandPayload>,
                stateType = handler.stateType as KClass<out Any>?,
            )
        }

    private val eventHandlersIndex: List<EventHandlerEntry> =
        eventHandlers.map { handler ->
            @Suppress("UNCHECKED_CAST")
            EventHandlerEntry(
                handler = handler,
                payloadType = handler.payloadType as KClass<out EventPayload>,
                stateType = handler.stateType as KClass<out Any>?,
            )
        }

    fun processCommand(command: CommandMessage<out CommandPayload>, currentState: Any? = null): MessageHandlingResult.Command {
        val entry = findCommandHandler(command)
        return entry.handle(command, currentState, log)
    }

    fun processEvent(event: EventMessage<out EventPayload>, currentState: Any? = null): MessageHandlingResult.Event {
        val entry = findEventHandler(event)
        if (entry == null) {
            log.debug("No handler registered for event payload {}", event.payload::class.qualifiedName)
            return MessageHandlingResult.EventIgnored
        }
        return entry.handle(event, currentState, log)
    }

    private fun findCommandHandler(command: CommandMessage<out CommandPayload>): CommandHandlerEntry {
        val payloadClass = command.payload::class
        val exact = commandHandlersIndex.firstOrNull { it.payloadType == payloadClass }

        return exact
            ?: commandHandlersIndex.firstOrNull { it.payloadType.java.isAssignableFrom(payloadClass.java) }
            ?: error("No command handler registered for payload type ${payloadClass.qualifiedName}")
    }

    private fun findEventHandler(event: EventMessage<out EventPayload>): EventHandlerEntry? {
        val payloadClass = event.payload::class
        val exact = eventHandlersIndex.firstOrNull { it.payloadType == payloadClass }

        return exact
            ?: eventHandlersIndex.firstOrNull { it.payloadType.java.isAssignableFrom(payloadClass.java) }
    }

    private data class CommandHandlerEntry(
        val handler: CommandMessageHandler<*, *>,
        val payloadType: KClass<out CommandPayload>,
        val stateType: KClass<out Any>?,
    ) {
        fun handle(command: CommandMessage<out CommandPayload>, state: Any?, log: Logger): MessageHandlingResult.Command {
            val typedCommand = castCommand(command)
            val typedState = castState(state)
            val context = commandContext(typedState)

            val typedHandler = asHandler()

            if (!typedHandler.isMessageValid(typedCommand, context)) {
                log.debug("Command {} failed validation", typedCommand.payload::class.simpleName)
                return MessageHandlingResult.CommandIgnored
            }

            return typedHandler.handleMessage(typedCommand, context)
        }

        private fun castCommand(command: CommandMessage<out CommandPayload>): CommandMessage<CommandPayload> {
            val payload = command.payload
            if (!payloadType.java.isInstance(payload)) {
                error(
                    "Command payload type mismatch: expected ${payloadType.qualifiedName}, actual ${payload::class.qualifiedName}",
                )
            }

            @Suppress("UNCHECKED_CAST")
            return command as CommandMessage<CommandPayload>
        }

        private fun castState(state: Any?): Any? {
            if (state == null || stateType == null) {
                return null
            }

            if (!stateType.java.isInstance(state)) {
                throw IllegalArgumentException(
                    "State type mismatch: expected ${stateType.qualifiedName}, actual ${state::class.qualifiedName}",
                )
            }

            return state
        }

        @Suppress("UNCHECKED_CAST")
        private fun asHandler(): CommandMessageHandler<CommandPayload, Any> =
            handler as CommandMessageHandler<CommandPayload, Any>

        @Suppress("UNCHECKED_CAST")
        private fun commandContext(state: Any?): CommandContext<Any> =
            CommandContext<Any>(state as Any?)
    }

    private data class EventHandlerEntry(
        val handler: EventMessageHandler<*, *>,
        val payloadType: KClass<out EventPayload>,
        val stateType: KClass<out Any>?,
    ) {
        fun handle(event: EventMessage<out EventPayload>, state: Any?, log: Logger): MessageHandlingResult.Event {
            val typedEvent = castEvent(event)
            val typedState = castState(state)
            val context = eventContext(typedState)

            val typedHandler = asHandler()

            if (!typedHandler.isMessageValid(typedEvent, context)) {
                log.debug("Event {} failed validation", typedEvent.payload::class.simpleName)
                return MessageHandlingResult.EventIgnored
            }

            return typedHandler.handleMessage(typedEvent, context)
        }

        private fun castEvent(event: EventMessage<out EventPayload>): EventMessage<EventPayload> {
            val payload = event.payload
            if (!payloadType.java.isInstance(payload)) {
                error(
                    "Event payload type mismatch: expected ${payloadType.qualifiedName}, actual ${payload::class.qualifiedName}",
                )
            }

            @Suppress("UNCHECKED_CAST")
            return event as EventMessage<EventPayload>
        }

        private fun castState(state: Any?): Any? {
            if (state == null || stateType == null) {
                return null
            }

            if (!stateType.java.isInstance(state)) {
                throw IllegalArgumentException(
                    "State type mismatch: expected ${stateType.qualifiedName}, actual ${state::class.qualifiedName}",
                )
            }

            return state
        }

        @Suppress("UNCHECKED_CAST")
        private fun asHandler(): EventMessageHandler<EventPayload, Any> =
            handler as EventMessageHandler<EventPayload, Any>

        @Suppress("UNCHECKED_CAST")
        private fun eventContext(state: Any?): EventContext<Any> =
            EventContext<Any>(state as Any?)
    }
}