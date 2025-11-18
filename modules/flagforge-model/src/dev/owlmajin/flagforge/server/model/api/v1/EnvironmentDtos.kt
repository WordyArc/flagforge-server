package dev.owlmajin.flagforge.server.model.api.v1

import jakarta.validation.constraints.NotBlank


data class CreateEnvironmentRequest(
    @NotBlank
    val key: String,
    @NotBlank
    val name: String,
)

data class EnvironmentResource(
    val resourceName: String,
    val environmentId: String,
    val projectId: String,
    val key: String,
    val name: String,
    val version: Long,
)

fun environmentResourceName(
    projectId: String,
    environmentKey: String,
): String = "projects/$projectId/environments/$environmentKey"
