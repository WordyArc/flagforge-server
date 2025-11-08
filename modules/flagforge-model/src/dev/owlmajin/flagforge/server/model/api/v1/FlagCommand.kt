package dev.owlmajin.flagforge.server.model.api.v1

import dev.owlmajin.flagforge.server.model.CommandId
import dev.owlmajin.flagforge.server.model.FlagId
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
    val commandId: CommandId,
    val flagId: FlagId,
    val resourceName: String,
    val status: String = "ACCEPTED", // TODO в enum
)
