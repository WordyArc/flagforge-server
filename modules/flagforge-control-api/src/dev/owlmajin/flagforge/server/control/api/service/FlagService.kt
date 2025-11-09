package dev.owlmajin.flagforge.server.control.api.service

import dev.owlmajin.flagforge.server.common.IdGenerator
import dev.owlmajin.flagforge.server.model.CreateFlagCommand
import dev.owlmajin.flagforge.server.model.api.v1.CommandResponse
import dev.owlmajin.flagforge.server.model.api.v1.CreateFlagRequest
import dev.owlmajin.flagforge.server.model.api.v1.flagResourceName
import dev.owlmajin.flagforge.server.persistence.repository.FlagRepository
import org.springframework.stereotype.Service


@Service
class FlagService(
    private val idGenerator: IdGenerator,
    private val flagRepository: FlagRepository,
) {

    suspend fun createFlag(
        projectId: String,
        environmentKey: String,
        actorId: String,
        request: CreateFlagRequest
    ): CommandResponse {
        val flagId = idGenerator.next()

        val command = CreateFlagCommand(
            actorId = actorId,
            flagId = flagId,
            projectId = projectId,
            environmentKey = environmentKey,
            flagKey = request.key,
            type = request.type,
            enabled = request.enabled,
            rules = request.rules,
            defaultVariant = request.defaultVariant,
            salt = request.salt ?: flagId,
        )
        flagRepository.create(command)

        return CommandResponse(
            commandId = command.id,
            resourceName = flagResourceName(
                projectId = projectId,
                environmentKey = environmentKey,
                flagId = flagId,
            ),
        )
    }

}
