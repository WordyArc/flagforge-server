package dev.owlmajin.flagforge.server.control.api.webmvc.v1

import dev.owlmajin.flagforge.server.control.api.service.EnvironmentService
import dev.owlmajin.flagforge.server.control.api.webmvc.ACTOR_HEADER
import dev.owlmajin.flagforge.server.control.api.webmvc.API
import dev.owlmajin.flagforge.server.control.api.webmvc.CONTROL
import dev.owlmajin.flagforge.server.control.api.webmvc.V_1
import dev.owlmajin.flagforge.server.control.api.webmvc.resolveActorId
import dev.owlmajin.flagforge.server.model.api.v1.CommandResponse
import dev.owlmajin.flagforge.server.model.api.v1.CreateEnvironmentRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(path = [API + V_1 + CONTROL])
class EnvironmentController(private val environmentService: EnvironmentService) {

    @PostMapping(path = ["/projects/{projectId}/envs"])
    suspend fun createEnvironment(
        @RequestHeader(name = ACTOR_HEADER, required = false) actorHeader: String?,
        @PathVariable projectId: String,
        @Valid @RequestBody request: CreateEnvironmentRequest,
    ): ResponseEntity<CommandResponse> {
        val actorId = resolveActorId(actorHeader)

        val result = environmentService.createEnvironment(
            actorId = actorId,
            projectId = projectId,
            request = request,
        )

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result)
    }
}
