package dev.owlmajin.flagforge.server.control.api.service

import dev.owlmajin.flagforge.server.common.IdGenerator
import dev.owlmajin.flagforge.server.model.ActorId
import dev.owlmajin.flagforge.server.model.CreateCommand
import dev.owlmajin.flagforge.server.model.EnvironmentKey
import dev.owlmajin.flagforge.server.model.FlagKey
import dev.owlmajin.flagforge.server.model.ProjectId
import dev.owlmajin.flagforge.server.model.api.v1.CreateFlagRequest
import dev.owlmajin.flagforge.server.persistence.repository.FlagCommandRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class FlagCommandService(
    private val idGenerator: IdGenerator,
    private val flagCommandRepository: FlagCommandRepository,
) {

    suspend fun createFlag(
        projectId: ProjectId,
        environmentKey: EnvironmentKey,
        actor: ActorId,
        request: CreateFlagRequest
    ): CreateCommand {
        val commandId = idGenerator.nextCommandId()
        val flagId = idGenerator.nextFlagId()

        val command = CreateCommand(
            commandId = commandId,
            actor = actor,
            timestamp = Instant.now(),
            flagId = flagId,
            projectId = projectId,
            environmentKey = environmentKey,
            flagKey = FlagKey(request.key),
            type = request.type,
            enabled = request.enabled,
            rules = request.rules,
            defaultVariant = request.defaultVariant,
            salt = request.salt ?: flagId.value,
        )
        flagCommandRepository.send(command)

        return command
    }

}
