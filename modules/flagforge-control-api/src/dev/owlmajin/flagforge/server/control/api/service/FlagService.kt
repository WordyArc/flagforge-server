package dev.owlmajin.flagforge.server.control.api.service

import dev.owlmajin.flagforge.server.common.IdGenerator
import dev.owlmajin.flagforge.server.model.flag.CreateFlagCommand
import dev.owlmajin.flagforge.server.model.flag.toFlagCommandMessage
import dev.owlmajin.flagforge.server.model.flag.ToggleFlagCommand
import dev.owlmajin.flagforge.server.model.flag.FlagCommandPayload
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
            flagId = flagId,
            projectId = projectId,
            environmentKey = environmentKey,
            flagKey = request.key,
            type = request.type,
            enabled = request.enabled,
            rules = request.rules,
            defaultVariant = request.defaultVariant,
            salt = request.salt ?: flagId,
        ).toFlagCommandMessage(actorId)

        flagRepository.create(command)

        return CommandResponse(
            commandId = command.header.id,
            resourceName = flagResourceName(
                projectId = projectId,
                environmentKey = environmentKey,
                flagKey = request.key,
            ),
        )
    }

    suspend fun toggleFlag(
        flagId: String,
        actorId: String,
        expectedVersion: Long,
        enabled: Boolean,
    ): CommandResponse {
        val command = ToggleFlagCommand(
            flagId = flagId,
            expectedVersion = expectedVersion,
            enabled = enabled,
        ).toFlagCommandMessage(actorId)

        flagRepository.send(command)

        return CommandResponse(
            commandId = command.header.id,
            resourceName = "flags/$flagId",
        )
    }

}
