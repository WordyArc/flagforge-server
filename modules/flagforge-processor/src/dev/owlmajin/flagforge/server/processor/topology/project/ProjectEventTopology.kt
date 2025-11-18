package dev.owlmajin.flagforge.server.processor.topology.project

import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.model.project.ProjectEventPayload
import dev.owlmajin.flagforge.server.model.project.ProjectState
import dev.owlmajin.flagforge.server.processor.MessageProcessor
import dev.owlmajin.flagforge.server.processor.handling.EventResult
import dev.owlmajin.flagforge.server.processor.streams.eventsOf
import dev.owlmajin.flagforge.server.processor.streams.nullablePublishTo
import dev.owlmajin.flagforge.server.processor.streams.stream
import dev.owlmajin.flagforge.server.processor.streams.withState
import dev.owlmajin.flagforge.server.processor.topology.AbstractTopology
import dev.owlmajin.flagforge.server.processor.topology.flag.logIncomingEvents
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.kstream.KStream
import org.springframework.stereotype.Component

typealias ProjectEventStream = KStream<String, EventMessage<ProjectEventPayload>>
typealias ProjectEventResultStream = KStream<String, EventResult>

@Component
class ProjectEventTopology : AbstractTopology() {

    private val log = KotlinLogging.logger { javaClass }

    override fun configure() = with(builder) {
        log.info { "Configuring ProjectEventTopology" }

        val eventResults =
            stream(topics.events)
                .logIncomingEvents()
                .toProjectEvents()
                .processProjectEvents(projectState, messageProcessor)
                .skipIgnoredEvents()

        eventResults.persistProjectState()

        log.info { "ProjectEventTopology configured: events -> projectState" }
    }

    private fun KStream<String, Message<*>>.toProjectEvents(): ProjectEventStream =
        eventsOf<ProjectEventPayload>()

    private fun ProjectEventStream.processProjectEvents(
        projectState: org.apache.kafka.streams.kstream.KTable<String, ProjectState>,
        messageProcessor: MessageProcessor,
    ): ProjectEventResultStream =
        withState(projectState) { event, currentState ->
            messageProcessor.processEvent(event, currentState)
        }

    private fun ProjectEventResultStream.skipIgnoredEvents(): ProjectEventResultStream =
        filter { _, result -> result !is EventResult.Ignored }

    private fun ProjectEventResultStream.persistProjectState() {
        mapValues { result ->
            when (result) {
                is EventResult.Applied<*> -> result.newState as? ProjectState
                EventResult.Ignored -> null
            }
        }.nullablePublishTo(topics.projectState)
    }
}