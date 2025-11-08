package dev.owlmajin.flagforge.server.control.api.webmvc.v1

import dev.owlmajin.flagforge.server.control.api.service.FlagCommandService
import dev.owlmajin.flagforge.server.control.api.webmvc.API
import dev.owlmajin.flagforge.server.control.api.webmvc.V_1
import dev.owlmajin.flagforge.server.model.ActorId
import dev.owlmajin.flagforge.server.model.EnvironmentKey
import dev.owlmajin.flagforge.server.model.ProjectId
import dev.owlmajin.flagforge.server.model.api.v1.CreateFlagRequest
import dev.owlmajin.flagforge.server.model.api.v1.CreateFlagResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.uuid.Uuid

private const val CONTROL = "/control"

@RestController
@RequestMapping(path = [API + V_1 + CONTROL])
class FlagCommandController(private val flagCommandService: FlagCommandService) {

    @PostMapping(path = ["/projects/{projectId}/envs/{environmentKey}/flags"])
    suspend fun createFlag(
        @RequestHeader(name = "X-Actor-Id", required = false) actorHeader: String?,
        @PathVariable projectId: String,
        @PathVariable environmentKey: String,
        @Valid @RequestBody request: CreateFlagRequest,
    ): ResponseEntity<CreateFlagResponse> {
        val actor = ActorId(actorHeader?.takeIf { it.isNotBlank() } ?: "system")

        val result = flagCommandService.createFlag(
            projectId = ProjectId(projectId),
            environmentKey = EnvironmentKey(environmentKey),
            actor = actor,
            request = request,
        )
        val resourceName = "projects/$projectId/envs/$environmentKey/flags/${result.flagId.value}"

        return ResponseEntity.accepted().body(CreateFlagResponse(
            commandId = result.commandId,
            flagId = result.flagId,
            resourceName = resourceName
        ))
    }

}