package dev.owlmajin.flagforge.server.processor.handling.project

import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.project.ProjectCreatedEvent
import dev.owlmajin.flagforge.server.model.project.ProjectState
import dev.owlmajin.flagforge.server.processor.handling.EventContext
import dev.owlmajin.flagforge.server.processor.handling.EventHandler
import dev.owlmajin.flagforge.server.processor.handling.EventResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class ProjectCreatedEventHandler : EventHandler<ProjectCreatedEvent, ProjectState> {

    private val log = KotlinLogging.logger { javaClass }

    override val payloadType: KClass<out ProjectCreatedEvent> = ProjectCreatedEvent::class
    override val stateType: KClass<out ProjectState> = ProjectState::class

    override fun handleMessage(
        message: EventMessage<ProjectCreatedEvent>,
        context: EventContext<ProjectState>,
    ): EventResult {
        val current = context.currentState
        val payload = message.payload

        if (current != null) {
            log.warn {
                "ProjectCreated ignored: state already exists. projectId=${payload.projectId}, " +
                        "currentVersion=${current.version}"
            }
            return EventResult.Ignored
        }

        val state = ProjectState(
            id = payload.projectId,
            key = payload.key,
            name = payload.name,
            version = payload.version,
            updatedAt = message.header.timestamp,
        )

        return EventResult.Applied(current, state)
    }
}
