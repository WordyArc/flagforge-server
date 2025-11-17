package dev.owlmajin.flagforge.server.model.api.v1

import dev.owlmajin.flagforge.server.model.flag.FlagRule
import dev.owlmajin.flagforge.server.model.flag.FlagType
import java.time.Instant

data class FlagResource(
    val name: String, // "projects/{projectId}/environments/{envKey}/flags/{flagId}"
    val projectId: String,
    val environmentKey: String,
    val flagId: String,
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
    flagId: String,
): String = "projects/$projectId/environments/$environmentKey/flags/$flagId"