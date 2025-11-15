package dev.owlmajin.flagforge.server.processor.handling

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.CommandPayload
import dev.owlmajin.flagforge.server.model.EventMessage
import kotlin.reflect.KClass

data class CommandContext<S : Any>(
    val currentState: S?,
)

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

interface CommandHandler<P : CommandPayload, S : Any> {
    val payloadType: KClass<out P>
    val stateType: KClass<out S>?

    fun isMessageValid(message: CommandMessage<P>, context: CommandContext<S>): Boolean = true

    fun handleMessage(message: CommandMessage<P>, context: CommandContext<S>): CommandResult
}