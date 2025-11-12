package dev.owlmajin.flagforge.server.persistence.repository

import dev.owlmajin.flagforge.server.common.kafka.producer.OmniProducerFactory
import dev.owlmajin.flagforge.server.common.kafka.producer.sendAwait
import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.CreateProjectCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

interface ProjectRepository {
    suspend fun create(command: CommandMessage<CreateProjectCommand>)
}

@Component
class ProjectRepositoryImpl(
    private val persistenceProperties: PersistenceProperties,
    private val producerFactory: OmniProducerFactory,
) : ProjectRepository {

    companion object {
        private val log = LoggerFactory.getLogger(ProjectRepositoryImpl::class.java)
    }

    private val producer by lazy { producerFactory.createTopicProducer(persistenceProperties.commandMessages) }

    override suspend fun create(command: CommandMessage<CreateProjectCommand>) {
        val payload = command.payload
        producer.sendAwait(payload.projectId, command)

        log.debug(
            "Project command sent. type={}, projectId={}, commandId={}",
            payload::class.simpleName,
            payload.projectId,
            command.header.id,
        )
    }
}
