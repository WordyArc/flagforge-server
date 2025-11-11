package dev.owlmajin.flagforge.server.processor

import dev.owlmajin.flagforge.server.model.CommandRejectedEvent
import dev.owlmajin.flagforge.server.model.CreateFlagCommand
import dev.owlmajin.flagforge.server.model.DeleteFlagCommand
import dev.owlmajin.flagforge.server.model.FlagAggregate
import dev.owlmajin.flagforge.server.model.FlagCommand
import dev.owlmajin.flagforge.server.model.FlagCreatedEvent
import dev.owlmajin.flagforge.server.model.FlagDeletedEvent
import dev.owlmajin.flagforge.server.model.FlagEvent
import dev.owlmajin.flagforge.server.model.FlagRule
import dev.owlmajin.flagforge.server.model.FlagRulesUpdatedEvent
import dev.owlmajin.flagforge.server.model.FlagToggledEvent
import dev.owlmajin.flagforge.server.model.FlagType
import dev.owlmajin.flagforge.server.model.RuleAction
import dev.owlmajin.flagforge.server.model.ToggleFlagCommand
import dev.owlmajin.flagforge.server.model.UpdateFlagRulesCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.collections.mapNotNull


sealed interface FlagProcessingResult {

    data class Applied(
        val event: FlagEvent,
        val newState: FlagAggregate?,
    ) : FlagProcessingResult

    data class Rejected(
        val event: CommandRejectedEvent,
    ) : FlagProcessingResult
}

@Service
class MessageProcessor {

    private val log = LoggerFactory.getLogger(javaClass)

    fun process(command: FlagCommand, current: FlagAggregate?): FlagProcessingResult =
        when (command) {
            is CreateFlagCommand        -> handleCreate(command, current)
            is UpdateFlagRulesCommand   -> handleUpdateRules(command, current)
            is ToggleFlagCommand        -> handleToggle(command, current)
            is DeleteFlagCommand        -> handleDelete(command, current)
        }

    private fun handleCreate(
        command: CreateFlagCommand,
        current: FlagAggregate?,
    ): FlagProcessingResult {
        if (current != null) {
            log.debug("CreateFlag rejected: flag already exists. flagId={}", command.flagId)
            return reject(
                command = command,
                version = current.version,
                reason = "FLAG_ALREADY_EXISTS",
                errorCode = "FLAG_ALREADY_EXISTS",
            )
        }

        val version = 1L

        val event = FlagCreatedEvent(
            commandId = command.id,
            flagId = command.flagId,
            actorId = command.actorId ?: "system",
            version = version,
            projectId = command.projectId,
            environmentKey = command.environmentKey,
            flagKey = command.flagKey,
            type = command.type,
            enabled = command.enabled,
            rules = command.rules,
            defaultVariant = command.defaultVariant,
            salt = command.salt,
        )

        val state = FlagAggregate(
            id = command.flagId,
            projectId = command.projectId,
            environmentKey = command.environmentKey,
            key = command.flagKey,
            type = command.type,
            enabled = command.enabled,
            rules = command.rules,
            defaultVariant = command.defaultVariant,
            version = version,
            salt = command.salt,
            updatedAt = Instant.now(),
        )

        return FlagProcessingResult.Applied(event, state)
    }

    private fun handleUpdateRules(
        command: UpdateFlagRulesCommand,
        current: FlagAggregate?,
    ): FlagProcessingResult {
        if (current == null) {
            log.debug("UpdateFlagRules rejected: flag not found. flagId={}", command.flagId)
            return reject(
                command = command,
                version = 0L,
                reason = "FLAG_NOT_FOUND",
                errorCode = "FLAG_NOT_FOUND",
            )
        }

        if (command.expectedVersion != current.version) {
            log.debug(
                "UpdateFlagRules rejected: version mismatch. flagId={}, expected={}, actual={}",
                command.flagId,
                command.expectedVersion,
                current.version,
            )
            return reject(
                command = command,
                version = current.version,
                reason = "VERSION_MISMATCH",
                errorCode = "VERSION_MISMATCH",
            )
        }

        validateFlagDefinitionOrReject(
            command = command,
            type = current.type,
            rules = command.rules,
            defaultVariant = command.defaultVariant,
        )?.let { return it }

        val version = current.version + 1

        val event = FlagRulesUpdatedEvent(
            commandId = command.id,
            flagId = command.flagId,
            actorId = command.actorId ?: "system",
            version = version,
            rules = command.rules,
            defaultVariant = command.defaultVariant,
        )

        val state = current.copy(
            rules = command.rules,
            defaultVariant = command.defaultVariant,
            version = version,
            updatedAt = Instant.now(),
        )

        return FlagProcessingResult.Applied(event, state)
    }

    private fun handleToggle(
        command: ToggleFlagCommand,
        current: FlagAggregate?,
    ): FlagProcessingResult {
        if (current == null) {
            log.debug("ToggleFlag rejected: flag not found. flagId={}", command.flagId)
            return reject(
                command = command,
                version = 0L,
                reason = "FLAG_NOT_FOUND",
                errorCode = "FLAG_NOT_FOUND",
            )
        }

        if (command.expectedVersion != current.version) {
            log.debug(
                "ToggleFlag rejected: version mismatch. flagId={}, expected={}, actual={}",
                command.flagId,
                command.expectedVersion,
                current.version,
            )
            return reject(
                command = command,
                version = current.version,
                reason = "VERSION_MISMATCH",
                errorCode = "VERSION_MISMATCH",
            )
        }

        val version = current.version + 1

        val event = FlagToggledEvent(
            commandId = command.id,
            flagId = command.flagId,
            actorId = command.actorId ?: "system",
            version = version,
            enabled = command.enabled,
        )

        val state = current.copy(
            enabled = command.enabled,
            version = version,
            updatedAt = Instant.now(),
        )

        return FlagProcessingResult.Applied(event, state)
    }


    private fun handleDelete(
        command: DeleteFlagCommand,
        current: FlagAggregate?,
    ): FlagProcessingResult {
        if (current == null) {
            log.debug("DeleteFlag rejected: flag not found. flagId={}", command.flagId)
            return reject(
                command = command,
                version = 0L,
                reason = "FLAG_NOT_FOUND",
                errorCode = "FLAG_NOT_FOUND",
            )
        }

        if (command.expectedVersion != current.version) {
            log.debug(
                "DeleteFlag rejected: version mismatch. flagId={}, expected={}, actual={}",
                command.flagId,
                command.expectedVersion,
                current.version,
            )
            return reject(
                command = command,
                version = current.version,
                reason = "VERSION_MISMATCH",
                errorCode = "VERSION_MISMATCH",
            )
        }

        val version = current.version + 1

        val event = FlagDeletedEvent(
            commandId = command.id,
            flagId = command.flagId,
            actorId = command.actorId ?: "system",
            version = version,
        )

        // tombstone в compacted topic
        return FlagProcessingResult.Applied(event, newState = null)
    }

    private fun validateFlagDefinitionOrReject(
        command: FlagCommand,
        type: FlagType,
        rules: List<FlagRule>,
        defaultVariant: String?,
    ): FlagProcessingResult.Rejected? {
        validateRules(type, rules)?.let { code ->
            log.debug("Flag command rejected by rule validation. flagId={}, code={}", command.flagId, code)
            return reject(
                command = command,
                version = command.expectedVersion ?: 0L,
                reason = code,
                errorCode = code,
            )
        }

        validateDefaultVariant(type, rules, defaultVariant)?.let { code ->
            log.debug("Flag command rejected by defaultVariant validation. flagId={}, code={}", command.flagId, code)
            return reject(
                command = command,
                version = command.expectedVersion ?: 0L,
                reason = code,
                errorCode = code,
            )
        }

        return null
    }

}



private fun validateRules(
    type: FlagType,
    rules: List<FlagRule>,
): String? {
    if (rules.size > 100) return "TOO_MANY_RULES"

    val ids = rules.map { it.id }
    if (ids.size != ids.toSet().size) return "DUPLICATE_RULE_ID"

    val priorities = rules.map { it.priority }
    if (priorities.size != priorities.toSet().size) return "DUPLICATE_RULE_PRIORITY"
    if (priorities.any { it < 0 }) return "NEGATIVE_RULE_PRIORITY"

    rules.forEach { rule ->
        when (val action = rule.action) {
            is RuleAction.BooleanAction -> {
                if (type != FlagType.BOOLEAN) return "BOOLEAN_ACTION_FOR_NON_BOOLEAN_FLAG"
            }

            is RuleAction.PercentageAction -> {
                if (type != FlagType.PERCENTAGE) return "PERCENTAGE_ACTION_FOR_NON_PERCENTAGE_FLAG"
                if (action.truePercent !in 0..100) return "PERCENTAGE_OUT_OF_RANGE"
            }

            is RuleAction.MultiVariantAction -> {
                if (type != FlagType.MULTIVARIANT) return "MULTIVARIANT_ACTION_FOR_NON_MULTIVARIANT_FLAG"
                if (action.variants.isEmpty()) return "MULTIVARIANT_EMPTY"
                val percents = action.variants.values
                if (percents.any { it < 0 }) return "MULTIVARIANT_NEGATIVE_PERCENT"
                val sum = percents.sum()
                if (sum != 100) return "MULTIVARIANT_SUM_NOT_100"
            }
        }
    }

    return null
}

private fun validateDefaultVariant(
    type: FlagType,
    rules: List<FlagRule>,
    defaultVariant: String?,
): String? {
    return when (type) {
        FlagType.BOOLEAN -> {
            if (defaultVariant != null && defaultVariant !in setOf("true", "false")) {
                "INVALID_DEFAULT_VARIANT_FOR_BOOLEAN"
            } else null
        }

        FlagType.PERCENTAGE -> {
            if (defaultVariant != null) "DEFAULT_VARIANT_NOT_ALLOWED_FOR_PERCENTAGE" else null
        }

        FlagType.MULTIVARIANT -> {
            if (defaultVariant == null) return null
            val definedVariants = rules
                .mapNotNull { it.action as? RuleAction.MultiVariantAction }
                .flatMap { it.variants.keys }
                .toSet()
            if (defaultVariant !in definedVariants) "DEFAULT_VARIANT_NOT_IN_VARIANTS" else null
        }
    }
}

private fun reject(
    command: FlagCommand,
    version: Long,
    reason: String,
    errorCode: String,
): FlagProcessingResult.Rejected =
    FlagProcessingResult.Rejected(
        CommandRejectedEvent(
            commandId = command.id,
            flagId = command.flagId,
            actorId = command.actorId ?: "system",
            version = version,
            reason = reason,
            errorCode = errorCode,
        )
    )