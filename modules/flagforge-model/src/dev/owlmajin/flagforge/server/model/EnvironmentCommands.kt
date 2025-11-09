package dev.owlmajin.flagforge.server.model

import java.time.Instant
import kotlin.uuid.Uuid

sealed class EnvironmentCommand protected constructor(
    id: String,
    actorId: String,
    timestamp: Instant,
    expectedVersion: Long?,
    correlationId: String? = null,
    val environmentId: String,
) : Command(
    id = id,
    aggregateId = environmentId,
    aggregateType = AggregateType.ENVIRONMENT,
    actorId = actorId,
    timestamp = timestamp,
    correlationId = correlationId,
    expectedVersion = expectedVersion,
)

class CreateEnvironmentCommand(
    environmentId: String,
    actorId: String,
    val projectId: String,
    val key: String,
    val name: String,
    correlationId: String? = null,
) : EnvironmentCommand(
    id = Uuid.random().toString(),
    environmentId = environmentId,
    actorId = actorId,
    timestamp = Instant.now(),
    expectedVersion = null,
    correlationId = correlationId,
)
