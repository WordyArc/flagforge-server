package dev.owlmajin.flagforge.server.control.api.service

import dev.owlmajin.flagforge.server.common.IdGenerator
import dev.owlmajin.flagforge.server.model.CreateEnvironmentCommand
import dev.owlmajin.flagforge.server.model.EnvironmentCommand
import dev.owlmajin.flagforge.server.model.api.v1.CommandResponse
import dev.owlmajin.flagforge.server.model.api.v1.CreateEnvironmentRequest
import dev.owlmajin.flagforge.server.model.api.v1.environmentResourceName
import dev.owlmajin.flagforge.server.persistence.repository.EnvironmentRepository
import org.springframework.stereotype.Service

@Service
class EnvironmentService(
    private val idGenerator: IdGenerator,
    private val environmentRepository: EnvironmentRepository,
) {

    suspend fun createEnvironment(
        actorId: String,
        projectId: String,
        request: CreateEnvironmentRequest,
    ): CommandResponse {
        val environmentId = idGenerator.next()

        val command: EnvironmentCommand = CreateEnvironmentCommand(
            environmentId = environmentId,
            actorId = actorId,
            projectId = projectId,
            key = request.key,
            name = request.name,
        )

        environmentRepository.create(command)

        return CommandResponse(
            commandId = command.id,
            resourceName = environmentResourceName(projectId, request.key),
        )
    }
}
