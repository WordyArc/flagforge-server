package dev.owlmajin.flagforge.server.processor.topology.flag

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.FlagCommandPayload
import dev.owlmajin.flagforge.server.model.FlagEventPayload
import dev.owlmajin.flagforge.server.model.FlagState
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.processor.MessageProcessor
import dev.owlmajin.flagforge.server.processor.handling.CommandResult
import dev.owlmajin.flagforge.server.processor.handling.EventResult
import dev.owlmajin.flagforge.server.processor.topology.StreamsTopology
import dev.owlmajin.flagforge.server.processor.streams.Topics
import dev.owlmajin.flagforge.server.processor.streams.commandsOf
import dev.owlmajin.flagforge.server.processor.streams.eventsOf
import dev.owlmajin.flagforge.server.processor.streams.publishTo
import dev.owlmajin.flagforge.server.processor.streams.nullablePublishTo
import dev.owlmajin.flagforge.server.processor.streams.stream
import dev.owlmajin.flagforge.server.processor.streams.table
import dev.owlmajin.flagforge.server.processor.streams.withState
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.springframework.stereotype.Component

@Component
class FlagStreamsTopology(
    private val topics: Topics,
    private val messageProcessor: MessageProcessor,
) : StreamsTopology {

    private val klog = KotlinLogging.logger { javaClass }

    override fun configure(builder: StreamsBuilder) = with(builder) {
        val flagStateTable = materializeFlagState()

        commandFlow(flagStateTable)

        val eventResults = eventFlow(flagStateTable)
        persistFlagState(eventResults)

        rebuildFlagKeyIndex(eventResults)

        klog.info { "FlagStreamsTopology configured" }
    }


    private fun StreamsBuilder.materializeFlagState(): KTable<String, FlagState> {
        val flagState = table(topics.flagState)

        table(topics.projectState)
        table(topics.envState)
        table(topics.segmentState)

        klog.info { "Flag state materialized from topic ${topics.flagState.name}" }
        return flagState
    }

    // --- COMMAND PIPELINE ---

    private fun StreamsBuilder.commandFlow(
        flagState: KTable<String, FlagState>,
    ) {
        val incomingCommands: KStream<String, Message<*>> =
            stream(topics.commands)
                .logIncomingCommands()

        val commandResults: KStream<String, CommandResult> =
            incomingCommands
                .toFlagCommands()
                .processFlagCommands(flagState)
                .skipIgnoredCommands()

        commandResults
            .toFlagEvents()
            .publishTo(topics.events)
    }

    private fun KStream<String, Message<*>>.toFlagCommands(): KStream<String, CommandMessage<FlagCommandPayload>> =
        commandsOf<FlagCommandPayload>()

    private fun KStream<String, CommandMessage<FlagCommandPayload>>.processFlagCommands(flagState: KTable<String, FlagState>): KStream<String, CommandResult> =
        withState(flagState) { command, currentState -> messageProcessor.processCommand(command, currentState) }

    private fun KStream<String, CommandResult>.skipIgnoredCommands(): KStream<String, CommandResult> =
        filter { _, result -> result !is CommandResult.Ignored }

    private fun KStream<String, CommandResult>.toFlagEvents():
            KStream<String, Message<*>> =
        flatMapValues { result ->
            when (result) {
                is CommandResult.Applied -> listOf(result.event as Message<*>)
                is CommandResult.Rejected -> listOf(result.event as Message<*>)
                CommandResult.Ignored -> emptyList()
            }
        }

    // --- EVENT PIPELINE ---

    private fun StreamsBuilder.eventFlow(flagState: KTable<String, FlagState>): KStream<String, EventResult> {
        val incomingEvents: KStream<String, Message<*>> =
            stream(topics.events)
                .logIncomingEvents()

        return incomingEvents
            .toFlagEvents()
            .processFlagEvents(flagState)
            .skipIgnoredEvents()
    }

    private fun KStream<String, Message<*>>.toFlagEvents(): KStream<String, EventMessage<FlagEventPayload>> =
        eventsOf<FlagEventPayload>()

    private fun KStream<String, EventMessage<FlagEventPayload>>.processFlagEvents(flagState: KTable<String, FlagState>): KStream<String, EventResult> =
        withState(flagState) { event, currentState -> messageProcessor.processEvent(event, currentState) }

    private fun KStream<String, EventResult>.skipIgnoredEvents(): KStream<String, EventResult> =
        filter { _, result -> result !is EventResult.Ignored }



    // --- SINK ---

    private fun persistFlagState(eventResults: KStream<String, EventResult>) {
        eventResults
            .mapValues { result ->
                when (result) {
                    is EventResult.Applied<*> -> result.newState as? FlagState
                    EventResult.Ignored -> null
                }
            }
            .nullablePublishTo(topics.flagState)
    }

    private fun rebuildFlagKeyIndex(eventResults: KStream<String, EventResult>) {
        eventResults
            .flatMap { _, result ->
                when (result) {
                    is EventResult.Applied<*> -> {
                        val before = result.previousState as? FlagState
                        val after = result.newState as? FlagState

                        when {
                            // upsert
                            after != null -> listOf(
                                KeyValue(
                                    "${after.projectId}|${after.environmentKey}|${after.key}",
                                    after.id,
                                ),
                            )

                            // delete (tombstone по ключу)
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
            }
            .nullablePublishTo(topics.flagKeyIndex)
    }
}