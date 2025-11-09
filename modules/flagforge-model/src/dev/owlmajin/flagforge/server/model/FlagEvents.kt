package dev.owlmajin.flagforge.server.model

import java.time.Instant
import kotlin.uuid.Uuid


sealed class FlagEvent protected constructor(
    id: String,
    commandId: String,
    actorId: String,
    timestamp: Instant,
    version: Long,
    correlationId: String? = null,
    val flagId: String,
) : Event(
    id = id,
    aggregateId = flagId,
    aggregateType = AggregateType.FLAG,
    actorId = actorId,
    timestamp = timestamp,
    correlationId = correlationId,
    version = version,
    commandId = commandId,
)

class FlagCreatedEvent(
    commandId: String,
    flagId: String,
    actorId: String,
    version: Long,
    val projectId: String,
    val environmentKey: String,
    val flagKey: String,
    val type: FlagType,
    val enabled: Boolean,
    val rules: List<FlagRule>,
    val defaultVariant: String?,
    val salt: String,
) : FlagEvent(
    id = Uuid.random().toString(),
    commandId = commandId,
    flagId = flagId,
    actorId = actorId,
    timestamp = Instant.now(),
    version = version,
)

class FlagRulesUpdatedEvent(
    id: String,
    commandId: String,
    flagId: String,
    actorId: String,
    version: Long,
    val rules: List<FlagRule>,
    val defaultVariant: String?,
) : FlagEvent(
    id = Uuid.random().toString(),
    commandId = commandId,
    flagId = flagId,
    actorId = actorId,
    timestamp = Instant.now(),
    version = version,
)

class FlagToggledEvent(
    commandId: String,
    flagId: String,
    actorId: String,
    version: Long,
    val enabled: Boolean,
) : FlagEvent(
    id = Uuid.random().toString(),
    commandId = commandId,
    flagId = flagId,
    actorId = actorId,
    timestamp = Instant.now(),
    version = version,
)

class FlagDeletedEvent(
    commandId: String,
    flagId: String,
    actorId: String,
    version: Long,
) : FlagEvent(
    id = Uuid.random().toString(),
    commandId = commandId,
    flagId = flagId,
    actorId = actorId,
    timestamp = Instant.now(),
    version = version,
)

class CommandRejectedEvent(
    commandId: String,
    flagId: String,
    actorId: String,
    version: Long,
    val reason: String,
    val errorCode: String,
) : FlagEvent(
    id = Uuid.random().toString(),
    commandId = commandId,
    flagId = flagId,
    actorId = actorId,
    timestamp = Instant.now(),
    version = version,
)