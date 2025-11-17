package dev.owlmajin.flagforge.server.processor.handling.environment

import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.environment.EnvironmentCreatedEvent
import dev.owlmajin.flagforge.server.model.environment.EnvironmentState
import dev.owlmajin.flagforge.server.processor.handling.EventContext
import dev.owlmajin.flagforge.server.processor.handling.EventHandler
import dev.owlmajin.flagforge.server.processor.handling.EventResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class EnvironmentCreatedEventHandler : EventHandler<EnvironmentCreatedEvent, EnvironmentState> {

    private val log = KotlinLogging.logger { javaClass }

    override val payloadType: KClass<out EnvironmentCreatedEvent> = EnvironmentCreatedEvent::class
    override val stateType: KClass<out EnvironmentState> = EnvironmentState::class

    override fun handleMessage(
        message: EventMessage<EnvironmentCreatedEvent>,
        context: EventContext<EnvironmentState>,
    ): EventResult {
        val current = context.currentState
        val payload = message.payload

        if (current != null) {
            log.warn {
                "EnvironmentCreated ignored: state already exists. environmentId=${payload.environmentId}, " +
                        "currentVersion=${current.version}"
            }
            return EventResult.Ignored
        }

        val state = EnvironmentState(
            id = payload.environmentId,
            projectId = payload.projectId,
            key = payload.key,
            name = payload.name,
            version = payload.version,
            updatedAt = message.header.timestamp,
        )

        return EventResult.Applied(current, state)
    }
}
