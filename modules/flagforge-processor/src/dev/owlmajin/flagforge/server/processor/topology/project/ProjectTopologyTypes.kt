package dev.owlmajin.flagforge.server.processor.topology.project

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.model.project.ProjectCommandPayload
import dev.owlmajin.flagforge.server.model.project.ProjectEventPayload
import dev.owlmajin.flagforge.server.model.project.ProjectState
import dev.owlmajin.flagforge.server.processor.handling.CommandResult
import dev.owlmajin.flagforge.server.processor.handling.EventResult
import org.apache.kafka.streams.kstream.KStream

internal typealias ProjectRawMessageStream = KStream<String, Message<*>>
internal typealias ProjectCommandStream = KStream<String, CommandMessage<ProjectCommandPayload>>
internal typealias ProjectEventStream = KStream<String, EventMessage<ProjectEventPayload>>
internal typealias ProjectCommandResultStream = KStream<String, CommandResult>
internal typealias ProjectEventResultStream = KStream<String, EventResult>
internal typealias ProjectCommandContextStream = KStream<String, ProjectCommandContext>

data class ProjectCommandContext(
    val command: CommandMessage<ProjectCommandPayload>,
    val currentState: ProjectState?,
    val projectKeyOwnerId: String? = null,
) {
    fun withKeyOwner(ownerId: String?) = copy(projectKeyOwnerId = ownerId)
}
