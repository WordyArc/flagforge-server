package dev.owlmajin.flagforge.server.processor.topology.flag

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.FlagCommandPayload
import dev.owlmajin.flagforge.server.model.FlagState
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.processor.MessageProcessor
import dev.owlmajin.flagforge.server.processor.handling.CommandResult
import dev.owlmajin.flagforge.server.processor.streams.Topics
import dev.owlmajin.flagforge.server.processor.streams.commandsOf
import dev.owlmajin.flagforge.server.processor.streams.publishTo
import dev.owlmajin.flagforge.server.processor.streams.stream
import dev.owlmajin.flagforge.server.processor.streams.table
import dev.owlmajin.flagforge.server.processor.streams.withState
import dev.owlmajin.flagforge.server.processor.topology.StreamsTopology
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.springframework.stereotype.Component

@Component
class FlagCommandTopology(
    private val topics: Topics,
    private val messageProcessor: MessageProcessor,
): StreamsTopology {
    private val log = KotlinLogging.logger { javaClass }

    override fun configure(builder: StreamsBuilder) = with(builder) {
        log.info { "Configuring FlagCommandTopology" }
        val flagState = table(topics.flagState)

        stream(topics.commands)
            .logIncomingCommands()
            .toFlagCommands()
            .processFlagCommands(flagState, messageProcessor)
            .skipIgnoredCommands()
            .toFlagEvents()
            .publishTo(topics.events)

        log.info { "FlagCommandTopology configured: commands -> events" }
    }


    private fun KStream<String, Message<*>>.toFlagCommands(): KStream<String, CommandMessage<FlagCommandPayload>> =
        commandsOf<FlagCommandPayload>()

    private fun KStream<String, CommandMessage<FlagCommandPayload>>.processFlagCommands(
        flagState: KTable<String, FlagState>,
        messageProcessor: MessageProcessor,
    ): KStream<String, CommandResult> =
        withState(flagState) { command, currentState -> messageProcessor.processCommand(command, currentState) }

    private fun KStream<String, CommandResult>.skipIgnoredCommands(): KStream<String, CommandResult> =
        filter { _, result -> result !is CommandResult.Ignored }

    private fun KStream<String, CommandResult>.toFlagEvents(): KStream<String, Message<*>> =
        flatMapValues { result ->
            when (result) {
                is CommandResult.Applied -> listOf(result.event as Message<*>)
                is CommandResult.Rejected -> listOf(result.event as Message<*>)
                CommandResult.Ignored -> emptyList()
            }
        }
}