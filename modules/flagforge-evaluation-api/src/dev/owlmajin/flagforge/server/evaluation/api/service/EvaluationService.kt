package dev.owlmajin.flagforge.server.evaluation.api.service

import dev.owlmajin.flagforge.server.model.api.v1.EvaluationDto
import dev.owlmajin.flagforge.server.model.api.v1.EvaluationResult
import org.springframework.stereotype.Service

interface EvaluationService {
    fun evaluate(request: EvaluationDto): EvaluationResult
}

@Service
class EvaluationServiceImpl : EvaluationService {
    override fun evaluate(request: EvaluationDto): EvaluationResult {
        TODO()
    }
}
