package dev.owlmajin.flagforge.server.model.environment

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EnvironmentState(
    val id: String,
    val projectId: String,
    val key: String,
    val name: String,
    val version: Long,
    val updatedAt: Instant,
)