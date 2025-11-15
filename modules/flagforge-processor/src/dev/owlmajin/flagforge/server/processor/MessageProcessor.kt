package dev.owlmajin.flagforge.server.processor

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.CommandPayload
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.EventPayload
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.model.MessagePayload
import dev.owlmajin.flagforge.server.processor.handling.CommandContext
import dev.owlmajin.flagforge.server.processor.handling.CommandHandler
import dev.owlmajin.flagforge.server.processor.handling.CommandResult
import dev.owlmajin.flagforge.server.processor.handling.EventContext
import dev.owlmajin.flagforge.server.processor.handling.EventHandler
import dev.owlmajin.flagforge.server.processor.handling.EventResult
import dev.owlmajin.flagforge.server.processor.handling.requireTypeOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

@Service
class MessageProcessor(
    commandHandlers: List<CommandHandler<*, *>>,
    eventHandlers: List<EventHandler<*, *>>,
) {
    private val klog = KotlinLogging.logger { javaClass }

    private val commandRoutes: List<CommandRoute> =
        commandHandlers.map { it.asRoute() }
            .also { ensureUnique(it.map(CommandRoute::primaryType)) }

    private val eventRoutes: List<EventRoute> =
        eventHandlers.map { it.asRoute() }
            .also { ensureUnique(it.map(EventRoute::primaryType)) }

    fun processCommand(
        command: CommandMessage<out CommandPayload>,
        currentState: Any? = null,
    ): CommandResult {
        val route = commandRoutes.firstOrNull { it.supports(command.payload::class) }
            ?: error("No command handler registered for payload type ${command.payload::class.qualifiedName}")
        return route.execute(command, currentState)
    }

    fun processEvent(
        event: EventMessage<out EventPayload>,
        currentState: Any? = null,
    ): EventResult {
        val route = eventRoutes.firstOrNull { it.supports(event.payload::class) }
        if (route == null) {
            klog.debug { "No handler registered for event payload ${event.payload::class.qualifiedName}" }
            return EventResult.Ignored
        }
        return route.execute(event, currentState)
    }

    private interface CommandRoute {
        val primaryType: KClass<out CommandPayload>
        fun supports(payloadType: KClass<out CommandPayload>): Boolean
        fun execute(command: CommandMessage<out CommandPayload>, currentState: Any?): CommandResult
    }

    private interface EventRoute {
        val primaryType: KClass<out EventPayload>
        fun supports(payloadType: KClass<out EventPayload>): Boolean
        fun execute(event: EventMessage<out EventPayload>, currentState: Any?): EventResult
    }

    private fun <P : CommandPayload, S : Any> CommandHandler<P, S>.asRoute(): CommandRoute =
        object : CommandRoute {
            override val primaryType: KClass<out CommandPayload> = this@asRoute.payloadType

            override fun supports(payloadType: KClass<out CommandPayload>): Boolean =
                primaryType == payloadType || primaryType.java.isAssignableFrom(payloadType.java)

            override fun execute(
                command: CommandMessage<out CommandPayload>,
                currentState: Any?,
            ): CommandResult {
                val typedMessage = command.castPayload(this@asRoute.payloadType)
                val typedState = currentState.requireTypeOrNull(stateType)
                val context = CommandContext(typedState)

                return if (!isMessageValid(typedMessage, context)) {
                    klog.debug { "Command ${typedMessage.payload::class.simpleName} failed validation" }
                    CommandResult.Ignored
                } else {
                    handleMessage(typedMessage, context)
                }
            }
        }

    private fun <P : EventPayload, S : Any> EventHandler<P, S>.asRoute(): EventRoute =
        object : EventRoute {
            override val primaryType: KClass<out EventPayload> = this@asRoute.payloadType

            override fun supports(payloadType: KClass<out EventPayload>): Boolean =
                primaryType == payloadType || primaryType.java.isAssignableFrom(payloadType.java)

            override fun execute(
                event: EventMessage<out EventPayload>,
                currentState: Any?,
            ): EventResult {
                val typedEvent = event.castPayload(this@asRoute.payloadType)
                val typedState = currentState.requireTypeOrNull(stateType)
                val context = EventContext(typedState)

                return if (!isMessageValid(typedEvent, context)) {
                    klog.debug { "Event ${typedEvent.payload::class.simpleName} failed validation" }
                    EventResult.Ignored
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
