package dev.owlmajin.flagforge.server.control.api.webmvc.v1

import dev.owlmajin.flagforge.server.control.api.service.FlagService
import dev.owlmajin.flagforge.server.control.api.webmvc.ACTOR_HEADER
import dev.owlmajin.flagforge.server.control.api.webmvc.API
import dev.owlmajin.flagforge.server.control.api.webmvc.CONTROL
import dev.owlmajin.flagforge.server.control.api.webmvc.V_1
import dev.owlmajin.flagforge.server.control.api.webmvc.resolveActorId
import dev.owlmajin.flagforge.server.model.api.v1.CommandResponse
import dev.owlmajin.flagforge.server.model.api.v1.CreateFlagRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = [API + V_1 + CONTROL])
class FlagController(private val flagService: FlagService) {

    @PostMapping(path = ["/projects/{projectId}/envs/{environmentKey}/flags"])
    suspend fun createFlag(
        @RequestHeader(name = ACTOR_HEADER, required = false) actorHeader: String?,
        @PathVariable projectId: String,
        @PathVariable environmentKey: String,
        @Valid @RequestBody request: CreateFlagRequest,
    ): ResponseEntity<CommandResponse> {
        val actorId = resolveActorId(actorHeader)

        val result = flagService.createFlag(
            projectId = projectId,
            environmentKey = environmentKey,
            actorId = actorId,
            request = request,
        )

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result)
    }

}
