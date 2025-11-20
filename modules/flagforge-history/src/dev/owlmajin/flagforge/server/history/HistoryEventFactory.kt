package dev.owlmajin.flagforge.server.history

import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.EventPayload
import dev.owlmajin.flagforge.server.model.environment.EnvironmentEventPayload
import dev.owlmajin.flagforge.server.model.environment.EnvironmentState
import dev.owlmajin.flagforge.server.model.flag.FlagEventPayload
import dev.owlmajin.flagforge.server.model.flag.FlagState
import dev.owlmajin.flagforge.server.model.project.ProjectEventPayload
import dev.owlmajin.flagforge.server.model.project.ProjectState
import org.springframework.stereotype.Component

@Component
class HistoryEventFactory {

    fun project(
        event: EventMessage<out ProjectEventPayload>,
        before: ProjectState?,
        after: ProjectState?,
    ): HistoryEventEnvelope? = buildEnvelope(event, before, after) { previous, current ->
        ProjectHistoryPayload(previous, current)
    }

    fun environment(
        event: EventMessage<out EnvironmentEventPayload>,
        before: EnvironmentState?,
        after: EnvironmentState?,
    ): HistoryEventEnvelope? = buildEnvelope(event, before, after) { previous, current ->
        EnvironmentHistoryPayload(previous, current)
    }

    fun flag(
        event: EventMessage<out FlagEventPayload>,
        before: FlagState?,
        after: FlagState?,
    ): HistoryEventEnvelope? = buildEnvelope(event, before, after) { previous, current ->
        FlagHistoryPayload(previous, current)
    }

    private fun <S : Any> buildEnvelope(
        event: EventMessage<out EventPayload>,
        before: S?,
        after: S?,
        payloadFactory: (S?, S?) -> HistoryEventPayload,
    ): HistoryEventEnvelope? {
        if (before == null && after == null) {
            return null
        }

        val changeType = determineChangeType(before, after)
        val metadata = HistoryEventMetadata(
            eventId = event.header.id,
            aggregateId = event.header.aggregateId,
            aggregateType = event.header.aggregateType,
            changeType = changeType,
            version = event.payload.version,
            occurredAt = event.header.timestamp,
            actorId = event.header.actorId,
            commandId = event.payload.commandId,
            correlationId = event.header.correlationId,
            eventType = event.payload::class.simpleName
                ?: event.payload::class.qualifiedName
                ?: event.payload::class.java.name,
        )

        return HistoryEventEnvelope(
            metadata = metadata,
            payload = payloadFactory(before, after),
        )
    }
}
