package dev.owlmajin.flagforge.server.model.project

import com.fasterxml.jackson.annotation.JsonTypeName
import dev.owlmajin.flagforge.server.model.AggregateType
import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.CommandPayload
import dev.owlmajin.flagforge.server.model.commandMessage
import java.time.Instant
import kotlin.uuid.Uuid


sealed interface ProjectCommandPayload : CommandPayload {
    val projectId: String
}

@JsonTypeName("project.create")
data class CreateProjectCommand(
    override val projectId: String,
    val key: String,
    val name: String,
) : ProjectCommandPayload {
    override val expectedVersion: Long? = null
}

fun <T> T.toProjectCommandMessage(
    actorId: String,
    correlationId: String? = null,
    timestamp: Instant = Instant.now(),
    messageId: String = Uuid.random().toString(),
): CommandMessage<T> where T : ProjectCommandPayload =
    commandMessage(
        aggregateId = projectId,
        aggregateType = AggregateType.PROJECT,
        actorId = actorId,
        payload = this,
        timestamp = timestamp,
        correlationId = correlationId,
        messageId = messageId,
    )
