package dev.owlmajin.flagforge.server.processor.pipeline.flag

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.FlagCommandPayload
import dev.owlmajin.flagforge.server.model.FlagEventPayload
import dev.owlmajin.flagforge.server.model.FlagState
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.processor.MessageProcessor
import dev.owlmajin.flagforge.server.processor.handler.CommandResult
import dev.owlmajin.flagforge.server.processor.handler.EventResult
import dev.owlmajin.flagforge.server.processor.pipeline.StreamsPipeline
import dev.owlmajin.flagforge.server.processor.rocksdb.Topics
import dev.owlmajin.flagforge.server.processor.rocksdb.commandsOf
import dev.owlmajin.flagforge.server.processor.rocksdb.eventsOf
import dev.owlmajin.flagforge.server.processor.rocksdb.publishTo
import dev.owlmajin.flagforge.server.processor.rocksdb.nullablePublishTo
import dev.owlmajin.flagforge.server.processor.rocksdb.stream
import dev.owlmajin.flagforge.server.processor.rocksdb.table
import dev.owlmajin.flagforge.server.processor.rocksdb.withState
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.springframework.stereotype.Component

@Component
class FlagPipeline(
    private val topics: Topics,
    private val messageProcessor: MessageProcessor,
) : StreamsPipeline {

    private val klog = KotlinLogging.logger { javaClass }

    override fun build(builder: StreamsBuilder) {
        val flagState = builder.initStateTables()

        builder.stream(topics.commands)
            .logIncomingCommands()
            .extractFlagCommands()
            .handleFlagCommands(flagState)
            .dropIgnoredCommands()
            .also { it.publishFlagEvents() }

        val eventResults: KStream<String, EventResult> =
            builder.stream(topics.events)
                .logIncomingEvents()
                .handleFlagEvents(flagState)

        writeFlagState(eventResults)
        buildFlagIndex(eventResults)
    }


    private fun StreamsBuilder.initStateTables(): KTable<String, FlagState> {
        val flagState = table(topics.flagState)

        table(topics.projectState)
        table(topics.envState)
        table(topics.segmentState)

        return flagState
    }

    // --- COMMAND PIPELINE ---

    private fun KStream<String, Message<*>>.extractFlagCommands(): KStream<String, CommandMessage<FlagCommandPayload>> =
        commandsOf<FlagCommandPayload>()

    private fun KStream<String, CommandMessage<FlagCommandPayload>>.handleFlagCommands(flagState: KTable<String, FlagState>): KStream<String, CommandResult> =
        withState(flagState) { command, currentState -> messageProcessor.processCommand(command, currentState) }

    private fun KStream<String, CommandResult>.dropIgnoredCommands(): KStream<String, CommandResult> =
        filter { _, result -> result !is CommandResult.Ignored }

    private fun KStream<String, CommandResult>.publishFlagEvents() {
        val eventMessageSerde = topics.events.valueSerde
        flatMapValues<Message<*>> { result ->
            when (result) {
                is CommandResult.Applied -> listOf(result.event as Message<*>)
                is CommandResult.Rejected -> listOf(result.event as Message<*>)
                CommandResult.Ignored -> emptyList()
            }
        } publishTo topics.events.copy(valueSerde = eventMessageSerde)
    }

    // --- EVENT PIPELINE ---

    private fun KStream<String, Message<*>>.handleFlagEvents(
        flagState: KTable<String, FlagState>,
    ): KStream<String, EventResult> {
        val flagEvents: KStream<String, EventMessage<FlagEventPayload>> = eventsOf<FlagEventPayload>()

        return flagEvents.withState(flagState) { event, currentState -> messageProcessor.processEvent(event, currentState) }
            .filter { _, result -> result !is EventResult.Ignored }
    }

    // --- SINKS ---

    private fun writeFlagState(eventResults: KStream<String, EventResult>) {
        eventResults
            .mapValues { result ->
                when (result) {
                    is EventResult.Applied<*> -> result.newState as? FlagState
                    EventResult.Ignored -> null
                }
            } nullablePublishTo topics.flagState
    }

    private fun buildFlagIndex(eventResults: KStream<String, EventResult>) {
        eventResults
            .flatMap { _, result ->
                when (result) {
                    is EventResult.Applied<*> -> {
                        val before = result.previousState as? FlagState
                        val after = result.newState as? FlagState

                        when {
                            after != null -> listOf(
                                KeyValue(
                                    "${after.projectId}|${after.environmentKey}|${after.key}",
                                    after.id,
                                ),
                            )
                            before != null -> listOf(
                                KeyValue(
                                    "${before.projectId}|${before.environmentKey}|${before.key}",
                                    null,
                                ),
                            )
                            else -> emptyList()
                        }
                    }
                    EventResult.Ignored -> emptyList()
                }
            } nullablePublishTo topics.flagKeyIndex
    }
}