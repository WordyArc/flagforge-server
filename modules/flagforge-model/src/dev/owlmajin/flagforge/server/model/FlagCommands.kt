package dev.owlmajin.flagforge.server.model

import java.time.Instant

sealed interface FlagCommand {
    val commandId: CommandId
    val actor: ActorId
    val timestamp: Instant
    val flagId: FlagId
    val expectedVersion: Long?
}

data class CreateCommand(
    override val commandId: CommandId,
    override val actor: ActorId,
    override val timestamp: Instant,
    override val flagId: FlagId,
    val projectId: ProjectId,
    val environmentKey: EnvironmentKey,
    val flagKey: FlagKey,
    val type: FlagType,
    val enabled: Boolean,
    val rules: List<FlagRule>,
    val defaultVariant: String?,
    val salt: String,
): FlagCommand {
    override val expectedVersion: Long? = null
}

data class UploadFlagRulesCommand(
    override val commandId: CommandId,
    override val actor: ActorId,
    override val timestamp: Instant,
    override val flagId: FlagId,
    override val expectedVersion: Long,
    val rules: List<FlagRule>,
    val defaultVariant: String?
): FlagCommand

data class ToggleFlagCommand(
    override val commandId: CommandId,
    override val actor: ActorId,
    override val timestamp: Instant,
    override val flagId: FlagId,
    override val expectedVersion: Long,
    val enabled: Boolean,
): FlagCommand

data class DeleteFlagCommand(
    override val commandId: CommandId,
    override val actor: ActorId,
    override val timestamp: Instant,
    override val flagId: FlagId,
    override val expectedVersion: Long,
): FlagCommand

