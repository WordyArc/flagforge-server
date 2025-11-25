package dev.owlmajin.flagforge.server.processor.handling.flag

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.flag.FlagCommandPayload
import dev.owlmajin.flagforge.server.model.flag.FlagState
import dev.owlmajin.flagforge.server.model.flag.ToggleFlagCommand
import dev.owlmajin.flagforge.server.model.flag.FlagToggledEvent
import dev.owlmajin.flagforge.server.model.flag.CommandRejectedEvent
import dev.owlmajin.flagforge.server.model.flag.toFlagEventMessage
import dev.owlmajin.flagforge.server.processor.handling.CommandContext
import dev.owlmajin.flagforge.server.processor.handling.CommandHandler
import dev.owlmajin.flagforge.server.processor.handling.CommandResult
import java.time.Instant
import kotlin.reflect.KClass
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ToggleFlagHandler : AbstractFlagCommandHandler<ToggleFlagCommand>(ToggleFlagCommand::class) {

    override fun handle(
        message: CommandMessage<ToggleFlagCommand>,
        context: CommandContext<FlagState>,
    ): CommandResult {
        val payload = message.payload
        val current = context.currentState

        if (current == null) {
            log.debug("ToggleFlag rejected: flag not found. flagId={}", payload.flagId)
            return reject(
                command = message,
                version = 0L,
                reason = "FLAG_NOT_FOUND",
                errorCode = "FLAG_NOT_FOUND",
            )
        }

        if (payload.expectedVersion != current.version) {
            log.debug("ToggleFlag rejected: version mismatch. flagId={}, expected={}, current={}", payload.flagId, payload.expectedVersion, current.version)
            return reject(
                command = message,
                version = current.version,
                reason = "VERSION_MISMATCH",
                errorCode = "VERSION_MISMATCH",
            )
        }

        val event = FlagToggledEvent(
            flagId = payload.flagId,
            commandId = message.header.id,
            version = current.version + 1,
            enabled = payload.enabled,
        ).toFlagEvent(message)

        return CommandResult.Applied(event)
    }

}
