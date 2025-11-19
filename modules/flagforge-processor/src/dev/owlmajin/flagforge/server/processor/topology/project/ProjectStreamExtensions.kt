package dev.owlmajin.flagforge.server.processor.topology.project

import dev.owlmajin.flagforge.server.model.AggregateType
import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.model.MessageKind
import dev.owlmajin.flagforge.server.model.project.ProjectCommandPayload
import dev.owlmajin.flagforge.server.model.project.ProjectEventPayload

internal fun ProjectRawMessageStream.projectCommands(): ProjectCommandStream =
    filter { _, message -> message.header.aggregateType == AggregateType.PROJECT && message.header.kind == MessageKind.COMMAND }
        .mapValues { message -> message.toProjectCommand() }

internal fun ProjectRawMessageStream.projectEvents(): ProjectEventStream =
    filter { _, message -> message.header.aggregateType == AggregateType.PROJECT && message.header.kind == MessageKind.EVENT }
        .mapValues { message -> message.toProjectEvent() }

private fun Message<*>.toProjectCommand(): CommandMessage<ProjectCommandPayload> {
    val typedPayload = payload as? ProjectCommandPayload
        ?: error("Unexpected payload type ${this.payload::class.qualifiedName} for project command")
    return Message(header, typedPayload)
}

private fun Message<*>.toProjectEvent(): EventMessage<ProjectEventPayload> {
    val typedPayload = payload as? ProjectEventPayload
        ?: error("Unexpected payload type ${this.payload::class.qualifiedName} for project event")
    return Message(header, typedPayload)
}
