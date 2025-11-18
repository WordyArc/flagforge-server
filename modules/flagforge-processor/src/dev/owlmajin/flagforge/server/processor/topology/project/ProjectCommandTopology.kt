package dev.owlmajin.flagforge.server.processor.topology.project

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.model.project.ProjectCommandPayload
import dev.owlmajin.flagforge.server.model.project.ProjectState
import dev.owlmajin.flagforge.server.processor.MessageProcessor
import dev.owlmajin.flagforge.server.processor.handling.CommandResult
import dev.owlmajin.flagforge.server.processor.streams.commandsOf
import dev.owlmajin.flagforge.server.processor.streams.publishTo
import dev.owlmajin.flagforge.server.processor.topology.AbstractTopology
import dev.owlmajin.flagforge.server.processor.streams.stream
import dev.owlmajin.flagforge.server.processor.streams.withState
import dev.owlmajin.flagforge.server.processor.topology.flag.FlagRawMessageStream
import dev.owlmajin.flagforge.server.processor.topology.flag.logIncomingCommands
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.kstream.KStream
import org.springframework.stereotype.Component

typealias ProjectRawMessageStream = KStream<String, Message<*>>
typealias ProjectCommandStream = KStream<String, CommandMessage<ProjectCommandPayload>>
typealias ProjectCommandResultStream = KStream<String, CommandResult>

@Component
class ProjectCommandTopology : AbstractTopology() {

    private val log = KotlinLogging.logger { javaClass }

    override fun configure() = with(builder) {
        log.info { "Configuring ProjectCommandTopology" }

        stream(topics.commands)
            .logIncomingCommands()
            .toProjectCommands()
            .processProjectCommands(projectState, messageProcessor)
            .skipIgnoredCommands()
            .toProjectEvents()
            .publishTo(topics.events)

        log.info { "ProjectCommandTopology configured: commands -> events" }
    }

    private fun ProjectRawMessageStream.toProjectCommands(): ProjectCommandStream =
        commandsOf<ProjectCommandPayload>()

    private fun ProjectCommandStream.processProjectCommands(
        projectState: org.apache.kafka.streams.kstream.KTable<String, ProjectState>,
        messageProcessor: MessageProcessor,
    ): ProjectCommandResultStream =
        withState(projectState) { command, currentState ->
            messageProcessor.processCommand(command, currentState)
        }

    private fun ProjectCommandResultStream.skipIgnoredCommands(): ProjectCommandResultStream =
        filter { _, result -> result !is CommandResult.Ignored }

    private fun ProjectCommandResultStream.toProjectEvents(): FlagRawMessageStream =
        flatMapValues { result ->
            when (result) {
                is CommandResult.Applied -> listOf(result.event as Message<*>)
                is CommandResult.Rejected -> listOf(result.event as Message<*>)
                CommandResult.Ignored -> emptyList()
            }
        }
}
