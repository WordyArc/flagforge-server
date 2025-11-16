package dev.owlmajin.flagforge.server.evaluation.api.webflux

import dev.owlmajin.flagforge.server.common.AppDispatchers
import dev.owlmajin.flagforge.server.evaluation.api.API
import dev.owlmajin.flagforge.server.evaluation.api.EVAL
import dev.owlmajin.flagforge.server.evaluation.api.V_1
import dev.owlmajin.flagforge.server.evaluation.api.service.EvaluationService
import dev.owlmajin.flagforge.server.model.api.v1.EvaluationDto
import dev.owlmajin.flagforge.server.model.api.v1.EvaluationResult
import kotlinx.coroutines.withContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = [API + V_1 + EVAL])
class EvaluationController(
    private val evaluationService: EvaluationService,
    private val dispatchers: AppDispatchers,
    ) {

    @PostMapping
    suspend fun evaluate(
        @RequestBody request: EvaluationDto
    ): ResponseEntity<EvaluationResult> = withContext(dispatchers.default) {
        val result = evaluationService.evaluate(request)
        val status =
            if (result.reason == "FLAG_NOT_FOUND") HttpStatus.NOT_FOUND
            else HttpStatus.OK
        return@withContext ResponseEntity.status(status).body(result)
    }
}
