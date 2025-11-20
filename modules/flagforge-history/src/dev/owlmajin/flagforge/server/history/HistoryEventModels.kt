package dev.owlmajin.flagforge.server.history

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import dev.owlmajin.flagforge.server.model.AggregateType
import dev.owlmajin.flagforge.server.model.environment.EnvironmentState
import dev.owlmajin.flagforge.server.model.flag.FlagState
import dev.owlmajin.flagforge.server.model.project.ProjectState
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HistoryEventEnvelope(
    val metadata: HistoryEventMetadata,
    val payload: HistoryEventPayload,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HistoryEventMetadata(
    val eventId: String,
    val aggregateId: String,
    val aggregateType: AggregateType,
    val changeType: HistoryChangeType,
    val version: Long,
    val occurredAt: Instant,
    val actorId: String?,
    val commandId: String,
    val correlationId: String?,
    val eventType: String,
)

enum class HistoryChangeType {
    CREATED,
    UPDATED,
    DELETED,
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "payloadType",
)
sealed interface HistoryEventPayload

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProjectHistoryPayload(
    val before: ProjectState?,
    val after: ProjectState?,
) : HistoryEventPayload

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EnvironmentHistoryPayload(
    val before: EnvironmentState?,
    val after: EnvironmentState?,
) : HistoryEventPayload

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FlagHistoryPayload(
    val before: FlagState?,
    val after: FlagState?,
) : HistoryEventPayload

fun determineChangeType(before: Any?, after: Any?): HistoryChangeType = when {
    before == null && after != null -> HistoryChangeType.CREATED
    before != null && after == null -> HistoryChangeType.DELETED
    before != null && after != null -> HistoryChangeType.UPDATED
    else -> error("Unable to determine change type without state snapshots")
}

