package dev.owlmajin.flagforge.server.model.api.v1

import dev.owlmajin.flagforge.server.model.CreateFlagCommand
import dev.owlmajin.flagforge.server.model.FlagRule
import dev.owlmajin.flagforge.server.model.FlagType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateFlagRequest(
    @NotBlank
    val key: String,
    val type: FlagType,
    val enabled: Boolean = true,
    @Size(min = 1)
    val defaultVariant: String? = null,
    @Size(max = 100)
    val rules: List<FlagRule> = emptyList(), // TODO: дто использует доменную сущность
    val salt: String? = null,
)

data class CreateFlagResponse(
    val apiVersion: String = "v1",
    val status: String = "ACCEPTED", // TODO в enum
    val commandId: String,
    val flag: FlagResource
)

data class CreateFlagResult(
    val command: CreateFlagCommand,
    val resource: FlagResource,
)

data class CommandResponse(
    val apiVersion: String = "v1",
    val status: String = "ACCEPTED",
    val commandId: String,
    val resourceName: String,
)

data class SetFlagStateRequest(
    val enabled: Boolean,
    val expectedVersion: Long,
)

data class SetFlagRulesRequest(
    @field:Size(max = 100) val rules: List<FlagRule>,
    val defaultVariant: String?,
    val expectedVersion: Long,
)