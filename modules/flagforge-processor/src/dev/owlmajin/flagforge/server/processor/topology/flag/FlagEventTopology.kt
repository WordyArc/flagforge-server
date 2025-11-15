package dev.owlmajin.flagforge.server.processor.topology.flag

import dev.owlmajin.flagforge.server.model.FlagEventPayload
import dev.owlmajin.flagforge.server.model.FlagState
import dev.owlmajin.flagforge.server.processor.MessageProcessor
import dev.owlmajin.flagforge.server.processor.handling.EventResult
import dev.owlmajin.flagforge.server.processor.streams.TopicDescriptor
import dev.owlmajin.flagforge.server.processor.streams.Topics
import dev.owlmajin.flagforge.server.processor.streams.eventsOf
import dev.owlmajin.flagforge.server.processor.streams.nullablePublishTo
import dev.owlmajin.flagforge.server.processor.streams.stream
import dev.owlmajin.flagforge.server.processor.streams.withState
import dev.owlmajin.flagforge.server.processor.topology.StateTables
import dev.owlmajin.flagforge.server.processor.topology.StreamsTopology
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.KTable
import org.springframework.stereotype.Component

@Component
class FlagEventTopology(
    private val topics: Topics,
    private val messageProcessor: MessageProcessor,
    private val stateTables: StateTables,
) : StreamsTopology {

    private val log = KotlinLogging.logger { javaClass }

    override fun configure(builder: StreamsBuilder) = with(builder) {
        log.info { "Configuring FlagEventTopology" }

        val flagState: KTable<String, FlagState> = stateTables.flagState
        val eventResults =
            stream(topics.events)
                .logIncomingEvents()
                .toFlagEvents()
                .processFlagEvents(flagState, messageProcessor)
                .skipIgnoredEvents()

        eventResults.persistFlagState(topics.flagState)
        eventResults.rebuildFlagKeyIndex(topics.flagKeyIndex)

        log.info { "FlagEventTopology configured: events -> flagState + flagKeyIndex" }
    }

    private fun FlagRawMessageStream.toFlagEvents(): FlagEventStream =
        eventsOf<FlagEventPayload>()

    private fun FlagEventStream.processFlagEvents(
        flagState: KTable<String, FlagState>,
        messageProcessor: MessageProcessor,
    ): FlagEventResultStream =
        withState(flagState) { event, currentState -> messageProcessor.processEvent(event, currentState) }

    private fun FlagEventResultStream.skipIgnoredEvents(): FlagEventResultStream =
        filter { _, result -> result !is EventResult.Ignored }

// --- sinks ---

    private fun FlagEventResultStream.persistFlagState(flagStateTopic: TopicDescriptor<String, FlagState>) {
        mapValues { result ->
            when (result) {
                is EventResult.Applied<*> -> result.newState as? FlagState
                EventResult.Ignored -> null
            }
        }.nullablePublishTo(flagStateTopic)
    }

    private fun FlagEventResultStream.rebuildFlagKeyIndex(indexTopic: TopicDescriptor<String, String>) {
        flatMap { _, result ->
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
        }.nullablePublishTo(indexTopic)
    }
}
