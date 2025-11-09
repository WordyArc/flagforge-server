package dev.owlmajin.flagforge.server.model

import java.time.Instant
import kotlin.uuid.Uuid


sealed class FlagCommand protected constructor(
    id: String,
    actorId: String,
    timestamp: Instant,
    expectedVersion: Long?,
    correlationId: String? = null,
    val flagId: String,
) : Command(
    id = id,
    aggregateId = flagId,
    aggregateType = AggregateType.FLAG,
    actorId = actorId,
    timestamp = timestamp,
    correlationId = correlationId,
    expectedVersion = expectedVersion,
)

class CreateFlagCommand(
    flagId: String,
    actorId: String,
    correlationId: String? = null,
    val projectId: String,
    val environmentKey: String,
    val flagKey: String,
    val type: FlagType,
    val enabled: Boolean,
    val rules: List<FlagRule>,
    val defaultVariant: String?,
    val salt: String,
) : FlagCommand(
    id = Uuid.random().toString(),
    flagId = flagId,
    actorId = actorId,
    timestamp = Instant.now(),
    expectedVersion = null,
    correlationId = correlationId,
)

class UpdateFlagRulesCommand(
    flagId: String,
    actorId: String,
    expectedVersion: Long,
    correlationId: String? = null,
    val rules: List<FlagRule>,
    val defaultVariant: String?,
) : FlagCommand(
    id = Uuid.random().toString(),
    flagId = flagId,
    actorId = actorId,
    timestamp = Instant.now(),
    expectedVersion = expectedVersion,
    correlationId = correlationId,
)

class ToggleFlagCommand(
    flagId: String,
    actorId: String,
    expectedVersion: Long,
    val enabled: Boolean,
) : FlagCommand(
    id = Uuid.random().toString(),
    flagId = flagId,
    actorId = actorId,
    timestamp = Instant.now(),
    expectedVersion = expectedVersion,
)

class DeleteFlagCommand(
    flagId: String,
    actorId: String,
    expectedVersion: Long,
) : FlagCommand(
    id = Uuid.random().toString(),
    flagId = flagId,
    actorId = actorId,
    timestamp = Instant.now(),
    expectedVersion = expectedVersion,
)
