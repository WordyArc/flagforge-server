package dev.owlmajin.flagforge.server.model.environment

import com.fasterxml.jackson.annotation.JsonTypeName
import dev.owlmajin.flagforge.server.model.AggregateType
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.EventPayload
import dev.owlmajin.flagforge.server.model.eventMessage
import java.time.Instant
import kotlin.uuid.Uuid

sealed interface EnvironmentEventPayload : EventPayload {
    val environmentId: String
}

@JsonTypeName("environment.created")
data class EnvironmentCreatedEvent(
    override val environmentId: String,
    override val commandId: String,
    override val version: Long,
    val projectId: String,
    val key: String,
    val name: String,
) : EnvironmentEventPayload

fun <T> T.toEnvironmentEventMessage(
    actorId: String?,
    correlationId: String? = null,
    timestamp: Instant = Instant.now(),
    messageId: String = Uuid.random().toString(),
): EventMessage<T> where T : EnvironmentEventPayload = eventMessage(
    aggregateId = environmentId,
    aggregateType = AggregateType.ENVIRONMENT,
    actorId = actorId,
    payload = this,
    timestamp = timestamp,
    correlationId = correlationId,
    messageId = messageId,
)
