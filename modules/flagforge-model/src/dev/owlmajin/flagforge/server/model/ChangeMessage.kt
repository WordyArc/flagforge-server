package dev.owlmajin.flagforge.server.model

import java.time.Instant


sealed class ChangeMessage(
    val id: String,
    val aggregateId: String,
    val aggregateType: AggregateType,
    val actorId: String?,
    val timestamp: Instant,
    val correlationId: String?,
)

open class Command(
    id: String,
    aggregateId: String,
    aggregateType: AggregateType,
    actorId: String,
    timestamp: Instant,
    correlationId: String?,
    val expectedVersion: Long?,
) : ChangeMessage(
    id = id,
    aggregateId = aggregateId,
    aggregateType = aggregateType,
    actorId = actorId,
    timestamp = timestamp,
    correlationId = correlationId,
)

open class Event(
    id: String,
    aggregateId: String,
    aggregateType: AggregateType,
    actorId: String? = null,
    timestamp: Instant,
    correlationId: String? = null,
    val version: Long,
    val commandId: String,
) : ChangeMessage(
    id = id,
    aggregateId = aggregateId,
    aggregateType = aggregateType,
    actorId = actorId,
    timestamp = timestamp,
    correlationId = correlationId,
)

enum class AggregateType {
    PROJECT,
    ENVIRONMENT,
    SEGMENT,
    FLAG,
    SDK_KEY,
}
