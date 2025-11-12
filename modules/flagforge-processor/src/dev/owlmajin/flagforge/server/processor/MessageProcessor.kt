package dev.owlmajin.flagforge.server.processor

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.CommandPayload
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.EventPayload
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.model.MessagePayload
import dev.owlmajin.flagforge.server.processor.handler.CommandContext
import dev.owlmajin.flagforge.server.processor.handler.CommandMessageHandler
import dev.owlmajin.flagforge.server.processor.handler.EventContext
import dev.owlmajin.flagforge.server.processor.handler.EventMessageHandler
import dev.owlmajin.flagforge.server.processor.handler.MessageHandlingResult
import dev.owlmajin.flagforge.server.processor.handler.requireTypeOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

@Service
class MessageProcessor(
    commandHandlers: List<CommandMessageHandler<*, *>>,
    eventHandlers: List<EventMessageHandler<*, *>>,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val commandRoutes: List<CommandRoute> =
        commandHandlers.map { handler -> handler.asRoute(log) }
            .also { ensureUnique(it.map(CommandRoute::primaryType)) }

    private val eventRoutes: List<EventRoute> =
        eventHandlers.map { handler -> handler.asRoute(log) }
            .also { ensureUnique(it.map(EventRoute::primaryType)) }

    fun processCommand(command: CommandMessage<out CommandPayload>, currentState: Any? = null): MessageHandlingResult.Command {
        val route = commandRoutes.firstOrNull { it.supports(command.payload::class) }
            ?: error("No command handler registered for payload type ${command.payload::class.qualifiedName}")
        return route.execute(command, currentState)
    }

    fun processEvent(event: EventMessage<out EventPayload>, currentState: Any? = null): MessageHandlingResult.Event {
        val route = eventRoutes.firstOrNull { it.supports(event.payload::class) }
        if (route == null) {
            log.debug("No handler registered for event payload {}", event.payload::class.qualifiedName)
            return MessageHandlingResult.EventIgnored
        }
        return route.execute(event, currentState)
    }

    private interface CommandRoute {
        val primaryType: KClass<out CommandPayload>
        fun supports(payloadType: KClass<out CommandPayload>): Boolean
        fun execute(command: CommandMessage<out CommandPayload>, currentState: Any?): MessageHandlingResult.Command
    }

    private interface EventRoute {
        val primaryType: KClass<out EventPayload>
        fun supports(payloadType: KClass<out EventPayload>): Boolean
        fun execute(event: EventMessage<out EventPayload>, currentState: Any?): MessageHandlingResult.Event
    }

    private fun <P : CommandPayload, S : Any> CommandMessageHandler<P, S>.asRoute(log: Logger): CommandRoute =
        object : CommandRoute {
            override val primaryType: KClass<out CommandPayload> = this@asRoute.payloadType

            override fun supports(payloadType: KClass<out CommandPayload>): Boolean =
                primaryType == payloadType || primaryType.java.isAssignableFrom(payloadType.java)

            override fun execute(
                command: CommandMessage<out CommandPayload>,
                currentState: Any?,
            ): MessageHandlingResult.Command {
                val typedMessage = command.castPayload(this@asRoute.payloadType)
                val typedState = currentState.requireTypeOrNull(stateType)
                val context = CommandContext(typedState)

                return if (!isMessageValid(typedMessage, context)) {
                    log.debug("Command {} failed validation", typedMessage.payload::class.simpleName)
                    MessageHandlingResult.CommandIgnored
                } else {
                    handleMessage(typedMessage, context)
                }
            }
        }

    private fun <P : EventPayload, S : Any> EventMessageHandler<P, S>.asRoute(log: Logger): EventRoute =
        object : EventRoute {
            override val primaryType: KClass<out EventPayload> = this@asRoute.payloadType

            override fun supports(payloadType: KClass<out EventPayload>): Boolean =
                primaryType == payloadType || primaryType.java.isAssignableFrom(payloadType.java)

            override fun execute(
                event: EventMessage<out EventPayload>,
                currentState: Any?,
            ): MessageHandlingResult.Event {
                val typedEvent = event.castPayload(this@asRoute.payloadType)
                val typedState = currentState.requireTypeOrNull(stateType)
                val context = EventContext(typedState)

                return if (!isMessageValid(typedEvent, context)) {
                    log.debug("Event {} failed validation", typedEvent.payload::class.simpleName)
                    MessageHandlingResult.EventIgnored
                } else {
                    handleMessage(typedEvent, context)
                }
            }
        }

    private fun <P : MessagePayload> Message<out MessagePayload>.castPayload(expected: KClass<out P>): Message<P> {
        val typedPayload = expected.safeCast(payload)
            ?: error("Payload type mismatch: expected ${expected.qualifiedName}, actual ${payload::class.qualifiedName}")
        return Message(header, typedPayload)
    }

    private fun <T : Any> ensureUnique(types: List<KClass<out T>>) {
        val duplicates = types.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplicates.isEmpty()) {
            val names = duplicates.joinToString { it.qualifiedName ?: it.simpleName ?: it.toString() }
            "Duplicate handlers registered for payload types: $names"
        }
    }
}
