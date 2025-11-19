package dev.owlmajin.flagforge.server.processor.topology.environment

import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.model.environment.EnvironmentEventPayload
import dev.owlmajin.flagforge.server.model.environment.EnvironmentState
import dev.owlmajin.flagforge.server.processor.MessageProcessor
import dev.owlmajin.flagforge.server.processor.handling.EventResult
import dev.owlmajin.flagforge.server.processor.streams.eventsOf
import dev.owlmajin.flagforge.server.processor.streams.nullablePublishTo
import dev.owlmajin.flagforge.server.processor.streams.stream
import dev.owlmajin.flagforge.server.processor.streams.withState
import dev.owlmajin.flagforge.server.processor.topology.AbstractTopology
import dev.owlmajin.flagforge.server.processor.topology.logging.logIncomingEvents
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.kstream.KStream
import org.springframework.stereotype.Component


typealias EnvironmentEventStream = KStream<String, EventMessage<EnvironmentEventPayload>>
typealias EnvironmentEventResultStream = KStream<String, EventResult>

@Component
class EnvironmentEventTopology : AbstractTopology() {

    private val log = KotlinLogging.logger { javaClass }

    override fun configure() = with(builder) {
        log.info { "Configuring EnvironmentEventTopology" }

        val eventResults =
            stream(topics.events)
                .logIncomingEvents()
                .toEnvironmentEvents()
                .processEnvironmentEvents(envState, messageProcessor)
                .skipIgnoredEvents()

        eventResults.persistEnvironmentState()

        log.info { "EnvironmentEventTopology configured: events -> envState" }
    }

    private fun KStream<String, Message<*>>.toEnvironmentEvents(): EnvironmentEventStream =
        eventsOf<EnvironmentEventPayload>()

    private fun EnvironmentEventStream.processEnvironmentEvents(
        envState: org.apache.kafka.streams.kstream.KTable<String, EnvironmentState>,
        messageProcessor: MessageProcessor,
    ): EnvironmentEventResultStream =
        withState(envState) { event, currentState ->
            messageProcessor.processEvent(event, currentState)
        }

    private fun EnvironmentEventResultStream.skipIgnoredEvents(): EnvironmentEventResultStream =
        filter { _, result -> result !is EventResult.Ignored }

    private fun EnvironmentEventResultStream.persistEnvironmentState() {
        mapValues { result ->
            when (result) {
                is EventResult.Applied<*> -> result.newState as? EnvironmentState
                EventResult.Ignored -> null
            }
        }.nullablePublishTo(topics.envState)
    }
}
