package dev.owlmajin.flagforge.server.model.project

import com.fasterxml.jackson.annotation.JsonTypeName
import dev.owlmajin.flagforge.server.model.AggregateType
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.EventPayload
import dev.owlmajin.flagforge.server.model.eventMessage
import java.time.Instant
import kotlin.uuid.Uuid


sealed interface ProjectEventPayload : EventPayload {
    val projectId: String
}

@JsonTypeName("project.created")
data class ProjectCreatedEvent(
    override val projectId: String,
    override val commandId: String,
    override val version: Long,
    val key: String,
    val name: String,
) : ProjectEventPayload

fun <T> T.toProjectEventMessage(
    actorId: String?,
    correlationId: String? = null,
    timestamp: Instant = Instant.now(),
    messageId: String = Uuid.random().toString(),
): EventMessage<T> where T : ProjectEventPayload = eventMessage(
    aggregateId = projectId,
    aggregateType = AggregateType.PROJECT,
    actorId = actorId,
    payload = this,
    timestamp = timestamp,
    correlationId = correlationId,
    messageId = messageId,
)