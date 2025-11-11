package dev.owlmajin.flagforge.server.persistence.repository

import dev.owlmajin.flagforge.server.common.kafka.producer.OmniProducerFactory
import dev.owlmajin.flagforge.server.common.kafka.producer.sendAwait
import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.model.ProjectCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

interface ProjectRepository {
    suspend fun create(command: ProjectCommand)
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

    override suspend fun create(command: ProjectCommand) {
        producer.sendAwait(command.projectId, command)

        log.debug("Project command sent. type=${command::class.simpleName}, projectId=${command.projectId}, commandId=${command.id}")
    }
}
