package dev.owlmajin.flagforge.server.processor.handling.project

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.project.CreateProjectCommand
import dev.owlmajin.flagforge.server.model.project.ProjectCommandPayload
import dev.owlmajin.flagforge.server.model.project.ProjectCreatedEvent
import dev.owlmajin.flagforge.server.model.project.ProjectEventPayload
import dev.owlmajin.flagforge.server.model.project.ProjectState
import dev.owlmajin.flagforge.server.model.project.toProjectEventMessage
import dev.owlmajin.flagforge.server.processor.handling.CommandContext
import dev.owlmajin.flagforge.server.processor.handling.CommandHandler
import dev.owlmajin.flagforge.server.processor.handling.CommandResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant
import kotlin.reflect.KClass

@Component
class CreateProjectHandler : CommandHandler<CreateProjectCommand, ProjectState> {

    private val log = KotlinLogging.logger { javaClass }

    override val payloadType: KClass<out CreateProjectCommand> = CreateProjectCommand::class
    override val stateType: KClass<out ProjectState> = ProjectState::class

    override fun handleMessage(
        message: CommandMessage<CreateProjectCommand>,
        context: CommandContext<ProjectState>,
    ): CommandResult {
        val current = context.currentState
        val payload = message.payload

        if (current != null) {
            // Для MVP делаем create идемпотентным:
            // если state уже есть - просто игнорируем команду.
            log.debug { "CreateProject ignored: project already exists. projectId=${payload.projectId}" }
            return CommandResult.Ignored
        }

        val event = ProjectCreatedEvent(
            projectId = payload.projectId,
            commandId = message.header.id,
            version = 1L,
            key = payload.key,
            name = payload.name,
        ).toProjectEvent(message)

        return CommandResult.Applied(event)
    }
}

private fun <T : ProjectEventPayload> T.toProjectEvent(
    command: CommandMessage<out ProjectCommandPayload>,
    timestamp: Instant = Instant.now(),
) = toProjectEventMessage(
    actorId = command.header.actorId,
    correlationId = command.header.correlationId,
    timestamp = timestamp,
)
