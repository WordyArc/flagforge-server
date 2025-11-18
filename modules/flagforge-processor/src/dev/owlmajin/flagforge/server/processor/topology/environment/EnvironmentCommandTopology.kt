package dev.owlmajin.flagforge.server.processor.topology.environment

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.model.environment.EnvironmentCommandPayload
import dev.owlmajin.flagforge.server.model.environment.EnvironmentState
import dev.owlmajin.flagforge.server.processor.MessageProcessor
import dev.owlmajin.flagforge.server.processor.handling.CommandResult
import dev.owlmajin.flagforge.server.processor.streams.commandsOf
import dev.owlmajin.flagforge.server.processor.streams.publishTo
import dev.owlmajin.flagforge.server.processor.streams.stream
import dev.owlmajin.flagforge.server.processor.streams.withState
import dev.owlmajin.flagforge.server.processor.topology.AbstractTopology
import dev.owlmajin.flagforge.server.processor.topology.flag.logIncomingCommands
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.kstream.KStream
import org.springframework.stereotype.Component

typealias EnvironmentRawMessageStream = KStream<String, Message<*>>
typealias EnvironmentCommandStream = KStream<String, CommandMessage<EnvironmentCommandPayload>>
typealias EnvironmentCommandResultStream = KStream<String, CommandResult>

@Component
class EnvironmentCommandTopology : AbstractTopology() {

    private val log = KotlinLogging.logger { javaClass }

    override fun configure() = with(builder) {
        log.info { "Configuring EnvironmentCommandTopology" }

        stream(topics.commands)
            .logIncomingCommands()
            .toEnvironmentCommands()
            .processEnvironmentCommands(envState, messageProcessor)
            .skipIgnoredCommands()
            .toEnvironmentEvents()
            .publishTo(topics.events)

        log.info { "EnvironmentCommandTopology configured: commands -> events" }
    }

    private fun EnvironmentRawMessageStream.toEnvironmentCommands(): EnvironmentCommandStream =
        commandsOf<EnvironmentCommandPayload>()

    private fun EnvironmentCommandStream.processEnvironmentCommands(
        envState: org.apache.kafka.streams.kstream.KTable<String, EnvironmentState>,
        messageProcessor: MessageProcessor,
    ): EnvironmentCommandResultStream =
        withState(envState) { command, currentState ->
            messageProcessor.processCommand(command, currentState)
        }

    private fun EnvironmentCommandResultStream.skipIgnoredCommands(): EnvironmentCommandResultStream =
        filter { _, result -> result !is CommandResult.Ignored }

    private fun EnvironmentCommandResultStream.toEnvironmentEvents(): EnvironmentRawMessageStream =
        flatMapValues { result ->
            when (result) {
                is CommandResult.Applied -> listOf(result.event as Message<*>)
                is CommandResult.Rejected -> listOf(result.event as Message<*>)
                CommandResult.Ignored -> emptyList()
            }
        }
}