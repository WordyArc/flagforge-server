package dev.owlmajin.flagforge.server.model.api.v1

import dev.owlmajin.flagforge.server.model.flag.FlagRule
import dev.owlmajin.flagforge.server.model.flag.FlagType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

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

data class FlagResource(
    val resourceName: String,
    val projectId: String,
    val environmentKey: String,
    val id: String,
    val key: String,
    val type: FlagType,
    val enabled: Boolean,
    val defaultVariant: String?,
    val rules: List<FlagRule>,
    val version: Long,
    val salt: String,
    val updatedAt: Instant,
)

fun flagResourceName(
    projectId: String,
    environmentKey: String,
    id: String,
): String = "projects/$projectId/environments/$environmentKey/flags/$id"