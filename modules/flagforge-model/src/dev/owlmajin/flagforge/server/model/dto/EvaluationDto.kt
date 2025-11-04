package dev.owlmajin.flagforge.server.model.dto

data class EvaluationDto(
    val projectKey: String,
    val environment: String,
    val flagKey: String,
    val uid: String,
    val attributes: Map<String, String> = emptyMap()
)

data class EvaluationResult(
    val projectKey: String,
    val environment: String,
    val flagKey: String,
    val enabled: Boolean,
    val variant: String?,
    val reason: String,
)
