package dev.owlmajin.flagforge.server.control.api.webmvc.v1

import dev.owlmajin.flagforge.server.control.api.service.ProjectService
import dev.owlmajin.flagforge.server.control.api.webmvc.ACTOR_HEADER
import dev.owlmajin.flagforge.server.control.api.webmvc.API
import dev.owlmajin.flagforge.server.control.api.webmvc.CONTROL
import dev.owlmajin.flagforge.server.control.api.webmvc.V_1
import dev.owlmajin.flagforge.server.control.api.webmvc.resolveActorId
import dev.owlmajin.flagforge.server.model.api.v1.CommandResponse
import dev.owlmajin.flagforge.server.model.api.v1.CreateProjectRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = [API + V_1 + CONTROL])
class ProjectController(private val projectService: ProjectService) {

    @PostMapping(path = ["/projects"])
    suspend fun createProject(
        @RequestHeader(name = ACTOR_HEADER, required = false) actorHeader: String?,
        @Valid @RequestBody request: CreateProjectRequest,
    ): ResponseEntity<CommandResponse> {
        val actorId = resolveActorId(actorHeader)
        val result = projectService.createProject(
            actorId = actorId,
            request = request,
        )

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result)
    }
}
