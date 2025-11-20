package dev.owlmajin.flagforge.server.processor.topology.project

import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.project.ProjectEventPayload
import dev.owlmajin.flagforge.server.model.project.ProjectState
import dev.owlmajin.flagforge.server.processor.MessageProcessor
import dev.owlmajin.flagforge.server.processor.handling.EventResult
import dev.owlmajin.flagforge.server.processor.streams.TopicDescriptor
import dev.owlmajin.flagforge.server.processor.streams.nullablePublishTo
import dev.owlmajin.flagforge.server.processor.streams.stream
import dev.owlmajin.flagforge.server.processor.streams.withState
import dev.owlmajin.flagforge.server.processor.topology.AbstractTopology
import dev.owlmajin.flagforge.server.processor.topology.logging.logIncomingEvents
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.KeyValue
import org.springframework.stereotype.Component

@Component
class ProjectEventTopology : AbstractTopology() {

    private val log = KotlinLogging.logger { javaClass }

    override fun configure() = with(builder) {
        log.info { "Configuring ProjectEventTopology" }

        val eventResults =
            stream(topics.events)
                .logIncomingEvents()
                .projectEvents()
                .processProjectEvents(projectState, messageProcessor)
                .skipIgnoredEvents()

        eventResults.persistProjectState(topics.projectState)
        eventResults.rebuildProjectKeyIndex(topics.projectKeyIndex)
        eventResults.publishProjectHistory()

        log.info { "ProjectEventTopology configured: events -> projectState" }
    }

    private fun ProjectEventStream.processProjectEvents(
        projectState: org.apache.kafka.streams.kstream.KTable<String, ProjectState>,
        messageProcessor: MessageProcessor,
    ): ProjectEventResultStream =
        withState(projectState) { event, currentState ->
            messageProcessor.processEvent(event, currentState)
        }

    private fun ProjectEventResultStream.skipIgnoredEvents(): ProjectEventResultStream =
        filter { _, result -> result !is EventResult.Ignored }

    private fun ProjectEventResultStream.persistProjectState(
        projectStateTopic: TopicDescriptor<String, ProjectState>,
    ) {
        mapValues { result ->
            when (result) {
                is EventResult.Applied<*> -> result.newState as? ProjectState
                EventResult.Ignored -> null
            }
        }.nullablePublishTo(projectStateTopic)
    }

    private fun ProjectEventResultStream.rebuildProjectKeyIndex(
        projectKeyIndexTopic: TopicDescriptor<String, String>,
    ) {
        flatMap { _, result ->
            when (result) {
                is EventResult.Applied<*> -> {
                    val before = result.previousState as? ProjectState
                    val after = result.newState as? ProjectState

                    when {
                        after != null -> listOf(KeyValue(after.key, after.id))
                        before != null -> listOf(KeyValue(before.key, null))
                        else -> emptyList()
                    }
                }

                EventResult.Ignored -> emptyList()
            }
        }.nullablePublishTo(projectKeyIndexTopic)
    }

    private fun ProjectEventResultStream.publishProjectHistory() {
        mapValues { result ->
            when (result) {
                is EventResult.Applied<*> -> historyEventFactory.project(
                    event = result.event as EventMessage<ProjectEventPayload>,
                    before = result.previousState as? ProjectState,
                    after = result.newState as? ProjectState,
                )

                EventResult.Ignored -> null
            }
        }
            .filter { _, envelope -> envelope != null }
            .mapValues { envelope -> envelope!! }
            .nullablePublishTo(topics.projectHistory)
    }
}