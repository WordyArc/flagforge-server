package dev.owlmajin.flagforge.server.processor.handling

import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.FlagCreatedEvent
import dev.owlmajin.flagforge.server.model.FlagEventPayload
import dev.owlmajin.flagforge.server.model.FlagState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass
import org.springframework.stereotype.Component

@Component
class FlagCreatedEventHandler : AbstractFlagEventHandler<FlagCreatedEvent>(FlagCreatedEvent::class) {

    override fun handle(
        message: EventMessage<FlagCreatedEvent>,
        context: EventContext<FlagState>,
    ): EventResult {
        val current = context.currentState
        val payload = message.payload

        if (current != null) {
            klog.warn { "FlagCreated ignored because state already exists. flagId=${payload.flagId}, currentVersion=${current.version}" }
            return EventResult.Ignored
        }

        val state = FlagState(
            id = payload.flagId,
            projectId = payload.projectId,
            environmentKey = payload.environmentKey,
            key = payload.flagKey,
            type = payload.type,
            enabled = payload.enabled,
            rules = payload.rules,
            defaultVariant = payload.defaultVariant,
            version = payload.version,
            salt = payload.salt,
            updatedAt = message.header.timestamp,
        )

        return EventResult.Applied(current, state)
    }
}

abstract class AbstractFlagEventHandler<T : FlagEventPayload>(
    override val payloadType: KClass<T>,
) : EventHandler<T, FlagState> {

    protected val klog = KotlinLogging.logger { javaClass }

    final override val stateType: KClass<out FlagState>? = FlagState::class

    final override fun handleMessage(
        message: EventMessage<T>,
        context: EventContext<FlagState>,
    ): EventResult = handle(message, context)

    protected abstract fun handle(
        message: EventMessage<T>,
        context: EventContext<FlagState>,
    ): EventResult
}