package dev.owlmajin.flagforge.server.processor.handling.environment

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.environment.CreateEnvironmentCommand
import dev.owlmajin.flagforge.server.model.environment.EnvironmentCommandPayload
import dev.owlmajin.flagforge.server.model.environment.EnvironmentCreatedEvent
import dev.owlmajin.flagforge.server.model.environment.EnvironmentEventPayload
import dev.owlmajin.flagforge.server.model.environment.EnvironmentState
import dev.owlmajin.flagforge.server.model.environment.toEnvironmentEventMessage
import dev.owlmajin.flagforge.server.processor.handling.CommandContext
import dev.owlmajin.flagforge.server.processor.handling.CommandHandler
import dev.owlmajin.flagforge.server.processor.handling.CommandResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant
import kotlin.reflect.KClass

@Component
class CreateEnvironmentHandler : CommandHandler<CreateEnvironmentCommand, EnvironmentState> {

    private val log = KotlinLogging.logger { javaClass }

    override val payloadType: KClass<out CreateEnvironmentCommand> = CreateEnvironmentCommand::class
    override val stateType: KClass<out EnvironmentState> = EnvironmentState::class

    override fun handleMessage(
        message: CommandMessage<CreateEnvironmentCommand>,
        context: CommandContext<EnvironmentState>,
    ): CommandResult {
        val current = context.currentState
        val payload = message.payload

        if (current != null) {
            log.debug(
                "CreateEnvironment ignored: environment already exists. environmentId={}",
                payload.environmentId,
            )
            return CommandResult.Ignored
        }

        // TODO: На этом этапе не проверяем существование projectState / уникальность key,
        //  добавить позже отдельной валидацией.
        val event = EnvironmentCreatedEvent(
            environmentId = payload.environmentId,
            commandId = message.header.id,
            version = 1L,
            projectId = payload.projectId,
            key = payload.key,
            name = payload.name,
        ).toEnvironmentEvent(message)

        return CommandResult.Applied(event)
    }
}

private fun <T : EnvironmentEventPayload> T.toEnvironmentEvent(
    command: CommandMessage<out EnvironmentCommandPayload>,
    timestamp: Instant = Instant.now(),
) = toEnvironmentEventMessage(
    actorId = command.header.actorId,
    correlationId = command.header.correlationId,
    timestamp = timestamp,
)