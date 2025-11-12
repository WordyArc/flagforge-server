package dev.owlmajin.flagforge.server.model

import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.Instant
import kotlin.uuid.Uuid


sealed interface EnvironmentCommandPayload : CommandPayload {
    val environmentId: String
}

@JsonTypeName("environment.create")
data class CreateEnvironmentCommand(
    override val environmentId: String,
    val projectId: String,
    val key: String,
    val name: String,
) : EnvironmentCommandPayload {
    override val expectedVersion: Long? = null
}

fun <T> T.toEnvironmentCommandMessage(
    actorId: String,
    correlationId: String? = null,
    timestamp: Instant = Instant.now(),
    messageId: String = Uuid.random().toString(),
): CommandMessage<T>
    where T : EnvironmentCommandPayload = commandMessage(
    aggregateId = environmentId,
    aggregateType = AggregateType.ENVIRONMENT,
    actorId = actorId,
    payload = this,
    timestamp = timestamp,
    correlationId = correlationId,
    messageId = messageId,
)
