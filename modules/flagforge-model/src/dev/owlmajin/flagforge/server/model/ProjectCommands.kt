package dev.owlmajin.flagforge.server.model

import java.time.Instant
import kotlin.uuid.Uuid

sealed class ProjectCommand protected constructor(
    id: String,
    actorId: String,
    timestamp: Instant,
    expectedVersion: Long?,
    correlationId: String? = null,
    val projectId: String,
) : Command(
    id = id,
    aggregateId = projectId,
    aggregateType = AggregateType.PROJECT,
    actorId = actorId,
    timestamp = timestamp,
    correlationId = correlationId,
    expectedVersion = expectedVersion,
)

class CreateProjectCommand(
    projectId: String,
    actorId: String,
    correlationId: String? = null,
    val key: String,
    val name: String,
) : ProjectCommand(
    id = Uuid.random().toString(),
    projectId = projectId,
    actorId = actorId,
    timestamp = Instant.now(),
    expectedVersion = null,
    correlationId = correlationId,
)
