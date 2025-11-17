package dev.owlmajin.flagforge.server.processor.handling

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.flag.CommandRejectedEvent
import dev.owlmajin.flagforge.server.model.flag.CreateFlagCommand
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.flag.FlagCommandPayload
import dev.owlmajin.flagforge.server.model.flag.FlagCreatedEvent
import dev.owlmajin.flagforge.server.model.flag.FlagEventPayload
import dev.owlmajin.flagforge.server.model.flag.FlagState
import dev.owlmajin.flagforge.server.model.flag.FlagType
import dev.owlmajin.flagforge.server.model.flag.RuleAction
import dev.owlmajin.flagforge.server.model.flag.toFlagEventMessage
import java.time.Instant
import kotlin.reflect.KClass
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CreateFlagHandler : AbstractFlagCommandHandler<CreateFlagCommand>(CreateFlagCommand::class) {


    override fun handle(
        message: CommandMessage<CreateFlagCommand>,
        context: CommandContext<FlagState>,
    ): CommandResult {
        val payload = message.payload
        val current = context.currentState

        if (current != null) {
            log.debug("CreateFlag rejected: flag already exists. flagId={}", payload.flagId)
            return reject(
                command = message,
                version = current.version,
                reason = "FLAG_ALREADY_EXISTS",
                errorCode = "FLAG_ALREADY_EXISTS",
            )
        }

        validateFlagDefinitionOrReject(message)?.let { return it }

        val event: EventMessage<FlagCreatedEvent> = FlagCreatedEvent(
            flagId = payload.flagId,
            commandId = message.header.id,
            version = 1L,
            projectId = payload.projectId,
            environmentKey = payload.environmentKey,
            flagKey = payload.flagKey,
            type = payload.type,
            enabled = payload.enabled,
            rules = payload.rules,
            defaultVariant = payload.defaultVariant,
            salt = payload.salt,
        ).toFlagEvent(message)

        return CommandResult.Applied(event)
    }

    private fun validateFlagDefinitionOrReject(
        command: CommandMessage<CreateFlagCommand>,
    ): CommandResult.Rejected? {
        val payload = command.payload
        validateCommonRules(payload)?.let { reason ->
            log.debug("CreateFlag rejected by common rule validation. flagId={}, code={}", payload.flagId, reason)
            return reject(
                command = command,
                version = 0L,
                reason = reason,
                errorCode = reason,
            )
        }

        val code = when (payload.type) {
            FlagType.BOOLEAN -> validateBooleanRules(payload)
            FlagType.PERCENTAGE -> TODO("Percentage flag not supported yet.")
            FlagType.MULTIVARIANT -> TODO("Multivariant flag not implemented fow now.")
        }

        return code?.let { reason ->
            log.debug("CreateFlag rejected by definition validation. flagId={}, code={}", payload.flagId, reason)
            reject(
                command = command,
                version = 0L,
                reason = reason,
                errorCode = reason,
            )
        }
    }

    private fun validateCommonRules(payload: CreateFlagCommand): String? {
        val rules = payload.rules

        if (rules.size > 100) return "TOO_MANY_RULES"

        val ids = rules.map { it.id }
        if (ids.size != ids.toSet().size) return "DUPLICATE_RULE_ID"

        val priorities = rules.map { it.priority }
        if (priorities.size != priorities.toSet().size) return "DUPLICATE_RULE_PRIORITY"
        if (priorities.any { it < 0 }) return "NEGATIVE_RULE_PRIORITY"

        return null
    }

    private fun validateBooleanRules(payload: CreateFlagCommand): String? {
        payload.rules.forEach { rule ->
            if (rule.action !is RuleAction.BooleanAction) {
                return "BOOLEAN_RULE_EXPECTED"
            }
        }

        val defaultVariant = payload.defaultVariant
        if (defaultVariant != null && defaultVariant !in setOf("true", "false")) {
            return "INVALID_DEFAULT_VARIANT_FOR_BOOLEAN"
        }

        return null
    }

    private fun validatePercentageRules(payload: CreateFlagCommand): String? {
        payload.rules.forEach { rule ->
            val action = rule.action
            if (action !is RuleAction.PercentageAction) {
                return "PERCENTAGE_RULE_EXPECTED"
            }
            if (action.truePercent !in 0..100) {
                return "PERCENTAGE_OUT_OF_RANGE"
            }
        }

        if (payload.defaultVariant != null) {
            return "DEFAULT_VARIANT_NOT_ALLOWED_FOR_PERCENTAGE"
        }

        return null
    }

}

abstract class AbstractFlagCommandHandler<T : FlagCommandPayload>(
    override val payloadType: KClass<T>,
) : CommandHandler<T, FlagState> {

    protected val log = LoggerFactory.getLogger(javaClass)

    final override val stateType: KClass<out FlagState> = FlagState::class

    final override fun handleMessage(
        message: CommandMessage<T>,
        context: CommandContext<FlagState>,
    ): CommandResult = handle(message, context)

    protected abstract fun handle(
        message: CommandMessage<T>,
        context: CommandContext<FlagState>,
    ): CommandResult

    protected fun <T : FlagEventPayload> T.toFlagEvent(
        command: CommandMessage<*>,
        timestamp: Instant = Instant.now(),
    ): EventMessage<T> = toFlagEventMessage(
        actorId = command.header.actorId,
        correlationId = command.header.correlationId,
        timestamp = timestamp,
    )

    protected fun reject(
        command: CommandMessage<out FlagCommandPayload>,
        version: Long,
        reason: String,
        errorCode: String,
    ): CommandResult.Rejected {
        val payload = CommandRejectedEvent(
            flagId = command.payload.flagId,
            commandId = command.header.id,
            version = version,
            reason = reason,
            errorCode = errorCode,
        )

        val event: EventMessage<CommandRejectedEvent> = payload.toFlagEvent(command)
        return CommandResult.Rejected(event)
    }
}