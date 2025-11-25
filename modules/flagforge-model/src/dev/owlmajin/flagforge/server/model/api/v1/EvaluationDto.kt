package dev.owlmajin.flagforge.server.model.api.v1

data class EvaluationDto(
    val projectId: String,
    val environmentKey: String,
    val flagKey: String,
    val uid: String,
    val attributes: Map<String, String> = emptyMap()
)

data class EvaluationResult(
    val projectId: String,
    val environmentKey: String,
    val flagKey: String,
    val flagId: String? = null,
    val enabled: Boolean,
    val variant: String?,
    val reason: String,
)
