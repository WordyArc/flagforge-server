package dev.owlmajin.flagforge.server.processor.topology.flag

import dev.owlmajin.flagforge.server.model.flag.FlagCommandPayload
import dev.owlmajin.flagforge.server.model.flag.FlagState
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.processor.MessageProcessor
import dev.owlmajin.flagforge.server.processor.handling.CommandResult
import dev.owlmajin.flagforge.server.processor.streams.commandsOf
import dev.owlmajin.flagforge.server.processor.streams.publishTo
import dev.owlmajin.flagforge.server.processor.streams.stream
import dev.owlmajin.flagforge.server.processor.streams.withState
import dev.owlmajin.flagforge.server.processor.topology.AbstractTopology
import dev.owlmajin.flagforge.server.processor.topology.logging.logIncomingCommands
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.kstream.KTable
import org.springframework.stereotype.Component

@Component
class FlagCommandTopology : AbstractTopology() {
    private val log = KotlinLogging.logger { javaClass }

    override fun configure() = with(builder) {
        log.info { "Configuring FlagCommandTopology" }
        stream(topics.commands)
            .logIncomingCommands()
            .toFlagCommands()
            .processFlagCommands(flagState, messageProcessor)
            .skipIgnoredCommands()
            .toFlagEvents()
            .publishTo(topics.events)

        log.info { "FlagCommandTopology configured: commands -> events" }
    }


    private fun FlagRawMessageStream.toFlagCommands(): FlagCommandStream =
        commandsOf<FlagCommandPayload>()

    private fun FlagCommandStream.processFlagCommands(
        flagState: KTable<String, FlagState>,
        messageProcessor: MessageProcessor,
    ): FlagCommandResultStream =
        withState(flagState) { command, currentState -> messageProcessor.processCommand(command, currentState) }

    private fun FlagCommandResultStream.skipIgnoredCommands(): FlagCommandResultStream =
        filter { _, result -> result !is CommandResult.Ignored }

    private fun FlagCommandResultStream.toFlagEvents(): FlagRawMessageStream =
        flatMapValues { result ->
            when (result) {
                is CommandResult.Applied -> listOf(result.event as Message<*>)
                is CommandResult.Rejected -> listOf(result.event as Message<*>)
                CommandResult.Ignored -> emptyList()
            }
        }
}