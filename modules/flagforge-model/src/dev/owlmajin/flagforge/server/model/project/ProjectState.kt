package dev.owlmajin.flagforge.server.model.project

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
class ProjectState(
    val id: String,
    val key: String,
    val name: String,
    val version: Long,
    val updatedAt: Instant,
)
