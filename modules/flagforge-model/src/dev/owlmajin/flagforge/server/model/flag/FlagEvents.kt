package dev.owlmajin.flagforge.server.model.flag

import com.fasterxml.jackson.annotation.JsonTypeName
import dev.owlmajin.flagforge.server.model.AggregateType
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.EventPayload
import dev.owlmajin.flagforge.server.model.eventMessage
import java.time.Instant
import kotlin.uuid.Uuid


sealed interface FlagEventPayload : EventPayload {
    val flagId: String
}

@JsonTypeName("flag.created")
data class FlagCreatedEvent(
    override val flagId: String,
    override val commandId: String,
    override val version: Long,
    val projectId: String,
    val environmentKey: String,
    val flagKey: String,
    val type: FlagType,
    val enabled: Boolean,
    val rules: List<FlagRule>,
    val defaultVariant: String?,
    val salt: String,
) : FlagEventPayload

@JsonTypeName("flag.rules-updated")
data class FlagRulesUpdatedEvent(
    override val flagId: String,
    override val commandId: String,
    override val version: Long,
    val rules: List<FlagRule>,
    val defaultVariant: String?,
) : FlagEventPayload

@JsonTypeName("flag.toggled")
data class FlagToggledEvent(
    override val flagId: String,
    override val commandId: String,
    override val version: Long,
    val enabled: Boolean,
) : FlagEventPayload

@JsonTypeName("flag.deleted")
data class FlagDeletedEvent(
    override val flagId: String,
    override val commandId: String,
    override val version: Long,
) : FlagEventPayload

@JsonTypeName("flag.command-rejected")
data class CommandRejectedEvent(
    override val flagId: String,
    override val commandId: String,
    override val version: Long,
    val reason: String,
    val errorCode: String,
) : FlagEventPayload

fun <T> T.toFlagEventMessage(
    actorId: String?,
    correlationId: String? = null,
    timestamp: Instant = Instant.now(),
    messageId: String = Uuid.random().toString(),
): EventMessage<T> where T : FlagEventPayload = eventMessage(
    aggregateId = flagId,
    aggregateType = AggregateType.FLAG,
    actorId = actorId,
    payload = this,
    timestamp = timestamp,
    correlationId = correlationId,
    messageId = messageId,
)