package dev.owlmajin.flagforge.server.processor.topology.project

import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.model.project.CreateProjectCommand
import dev.owlmajin.flagforge.server.model.project.ProjectCommandPayload
import dev.owlmajin.flagforge.server.model.project.ProjectCommandRejectedEvent
import dev.owlmajin.flagforge.server.model.project.ProjectState
import dev.owlmajin.flagforge.server.model.project.toProjectEventMessage
import dev.owlmajin.flagforge.server.processor.MessageProcessor
import dev.owlmajin.flagforge.server.processor.handling.CommandResult
import dev.owlmajin.flagforge.server.processor.streams.publishTo
import dev.owlmajin.flagforge.server.processor.streams.stream
import dev.owlmajin.flagforge.server.processor.streams.withState
import dev.owlmajin.flagforge.server.processor.topology.AbstractTopology
import dev.owlmajin.flagforge.server.processor.topology.logging.logIncomingCommands
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.kstream.GlobalKTable
import org.apache.kafka.streams.kstream.KTable
import org.springframework.stereotype.Component

@Component
class ProjectCommandTopology : AbstractTopology() {

    private val log = KotlinLogging.logger { javaClass }

    override fun configure() = with(builder) {
        log.info { "Configuring ProjectCommandTopology" }

        stream(topics.commands)
            .logIncomingCommands()
            .projectCommands()
            .toCommandContext()
            .withProjectKeyOwner(projectKeyIndex)
            .withProjectState(projectState)
            .routeThroughInvariants(messageProcessor)
            .skipIgnoredCommands()
            .toProjectEvents()
            .publishTo(topics.events)

        log.info { "ProjectCommandTopology configured: commands -> events" }
    }

    private fun ProjectCommandStream.toCommandContext(): ProjectCommandContextStream =
        mapValues { command -> ProjectCommandContext(command, currentState = null) }

    private fun ProjectCommandContextStream.withProjectKeyOwner(
        projectKeyIndex: GlobalKTable<String, String>,
    ): ProjectCommandContextStream =
        leftJoin(
            projectKeyIndex,
            { _, context -> context.command.payload.projectKeyOrNull() },
            { context, ownerId -> if (ownerId == null) context else context.withKeyOwner(ownerId) },
        )

    private fun ProjectCommandContextStream.withProjectState(
        projectState: KTable<String, ProjectState>,
    ): ProjectCommandContextStream =
        withState(projectState) { context, currentState ->
            context.copy(currentState = currentState)
        }

    private fun ProjectCommandContextStream.routeThroughInvariants(
        messageProcessor: MessageProcessor,
    ): ProjectCommandResultStream {
        val duplicates = filter { _, context -> context.isDuplicateProjectKey() }
        val accepted = filter { _, context -> !context.isDuplicateProjectKey() }

        val rejected = duplicates.mapValues { context: ProjectCommandContext -> context.toDuplicateKeyResult() }
        val processed = accepted.processProjectCommands(messageProcessor)

        return rejected.merge(processed)
    }

    private fun ProjectCommandContextStream.processProjectCommands(
        messageProcessor: MessageProcessor,
    ): ProjectCommandResultStream =
        mapValues { context ->
            messageProcessor.processCommand(context.command, context.currentState)
        }

    private fun ProjectCommandResultStream.skipIgnoredCommands(): ProjectCommandResultStream =
        filter { _, result -> result !is CommandResult.Ignored }

    private fun ProjectCommandResultStream.toProjectEvents(): ProjectRawMessageStream =
        flatMapValues { result ->
            when (result) {
                is CommandResult.Applied -> listOf(result.event as Message<*>)
                is CommandResult.Rejected -> listOf(result.event as Message<*>)
                CommandResult.Ignored -> emptyList()
            }
        }

    private fun ProjectCommandContext.isDuplicateProjectKey(): Boolean {
        val payload = command.payload
        return payload is CreateProjectCommand &&
                projectKeyOwnerId != null &&
                projectKeyOwnerId != payload.projectId
    }

    private fun ProjectCommandContext.toDuplicateKeyResult(): CommandResult {
        val payload = command.payload as CreateProjectCommand
        val conflictingProjectId = requireNotNull(projectKeyOwnerId) {
            "Duplicate key invariant triggered without owner"
        }

        val event = ProjectCommandRejectedEvent(
            projectId = payload.projectId,
            commandId = command.header.id,
            version = currentState?.version ?: 0L,
            key = payload.key,
            reason = DUPLICATE_PROJECT_KEY_REASON,
            message = "Project key '${payload.key}' already belongs to project '$conflictingProjectId'",
        ).toProjectEventMessage(
            actorId = command.header.actorId,
            correlationId = command.header.correlationId,
            timestamp = command.header.timestamp,
        )

        return CommandResult.Rejected(event)
    }

    private fun ProjectCommandPayload.projectKeyOrNull(): String? =
        when (this) {
            is CreateProjectCommand -> key
            else -> null
        }

    companion object {
        private const val DUPLICATE_PROJECT_KEY_REASON = "project-key-already-exists"
    }
}
