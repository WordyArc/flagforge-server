package dev.owlmajin.flagforge.server.model

import kotlin.time.Instant

sealed interface FlagEvent {
    val eventId: String
    val commandId: CommandId
    val flagId: FlagId
    val actor: ActorId
    val timestamp: Instant
    val version: Long
}

data class FlagCreatedEvent(
    override val eventId: String,
    override val commandId: CommandId,
    override val flagId: FlagId,
    override val actor: ActorId,
    override val timestamp: Instant,
    override val version: Long,
    val projectId: ProjectId,
    val environmentKey: EnvironmentKey,
    val flagKey: FlagKey,
    val type: FlagType,
    val enabled: Boolean,
    val rules: List<FlagRule>,
    val defaultVariant: String?,
    val salt: String
) : FlagEvent

data class FlagRulesChangedEvent(
    override val eventId: String,
    override val commandId: CommandId,
    override val flagId: FlagId,
    override val actor: ActorId,
    override val timestamp: Instant,
    override val version: Long,
    val rules: List<FlagRule>,
    val defaultVariant: String?
) : FlagEvent

data class FlagToggledEvent(
    override val eventId: String,
    override val commandId: CommandId,
    override val flagId: FlagId,
    override val actor: ActorId,
    override val timestamp: Instant,
    override val version: Long,
    val enabled: Boolean
) : FlagEvent

data class FlagDeletedEvent(
    override val eventId: String,
    override val commandId: CommandId,
    override val flagId: FlagId,
    override val actor: ActorId,
    override val timestamp: Instant,
    override val version: Long
) : FlagEvent

data class CommandRejectedEvent(
    override val eventId: String,
    override val commandId: CommandId,
    override val flagId: FlagId,
    override val actor: ActorId,
    override val timestamp: Instant,
    override val version: Long,
    val reason: String,
    val errorCode: String
) : FlagEvent