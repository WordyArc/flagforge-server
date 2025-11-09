package dev.owlmajin.flagforge.server.model.api.v1

import jakarta.validation.constraints.NotBlank


data class CreateProjectRequest(
    @NotBlank
    val key: String,
    @NotBlank
    val name: String,
)

data class ProjectResource(
    val name: String,
    val projectId: String,
    val key: String,
    val displayName: String,
    val version: Long,
)

fun projectResourceName(projectId: String): String =
    "projects/$projectId"