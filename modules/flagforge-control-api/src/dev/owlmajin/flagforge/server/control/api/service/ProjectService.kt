package dev.owlmajin.flagforge.server.control.api.service

import dev.owlmajin.flagforge.server.common.IdGenerator
import dev.owlmajin.flagforge.server.model.CreateProjectCommand
import dev.owlmajin.flagforge.server.model.ProjectCommand
import dev.owlmajin.flagforge.server.model.api.v1.CommandResponse
import dev.owlmajin.flagforge.server.model.api.v1.CreateProjectRequest
import dev.owlmajin.flagforge.server.model.api.v1.projectResourceName
import dev.owlmajin.flagforge.server.persistence.repository.ProjectRepository
import org.springframework.stereotype.Service

@Service
class ProjectService(
    private val idGenerator: IdGenerator,
    private val projectRepository: ProjectRepository,
) {

    suspend fun createProject(
        actorId: String,
        request: CreateProjectRequest,
    ): CommandResponse {
        val projectId = idGenerator.next()

        val command: ProjectCommand = CreateProjectCommand(
            projectId = projectId,
            actorId = actorId,
            key = request.key,
            name = request.name,
        )

        projectRepository.create(command)

        return CommandResponse(
            commandId = command.id,
            resourceName = projectResourceName(projectId),
        )
    }
}
