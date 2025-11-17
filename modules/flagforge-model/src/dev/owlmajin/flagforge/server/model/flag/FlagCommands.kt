package dev.owlmajin.flagforge.server.model.flag

import com.fasterxml.jackson.annotation.JsonTypeName
import dev.owlmajin.flagforge.server.model.AggregateType
import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.CommandPayload
import dev.owlmajin.flagforge.server.model.commandMessage
import java.time.Instant
import kotlin.uuid.Uuid


sealed interface FlagCommandPayload : CommandPayload {
    val flagId: String
}

@JsonTypeName("flag.create")
data class CreateFlagCommand(
    override val flagId: String,
    val projectId: String,
    val environmentKey: String,
    val flagKey: String,
    val type: FlagType,
    val enabled: Boolean,
    val rules: List<FlagRule>,
    val defaultVariant: String?,
    val salt: String,
) : FlagCommandPayload {
    override val expectedVersion: Long? = null
}

@JsonTypeName("flag.update-rules")
data class UpdateFlagRulesCommand(
    override val flagId: String,
    override val expectedVersion: Long,
    val rules: List<FlagRule>,
    val defaultVariant: String?,
) : FlagCommandPayload

@JsonTypeName("flag.toggle")
data class ToggleFlagCommand(
    override val flagId: String,
    override val expectedVersion: Long,
    val enabled: Boolean,
) : FlagCommandPayload

@JsonTypeName("flag.delete")
data class DeleteFlagCommand(
    override val flagId: String,
    override val expectedVersion: Long,
) : FlagCommandPayload

fun <T> T.toFlagCommandMessage(
    actorId: String,
    correlationId: String? = null,
    timestamp: Instant = Instant.now(),
    messageId: String = Uuid.random().toString(),
): CommandMessage<T> where T : FlagCommandPayload = commandMessage(
    aggregateId = flagId,
    aggregateType = AggregateType.FLAG,
    actorId = actorId,
    payload = this,
    timestamp = timestamp,
    correlationId = correlationId,
    messageId = messageId,
)
