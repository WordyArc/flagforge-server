package dev.owlmajin.flagforge.server.persistence.repository

import dev.owlmajin.flagforge.server.common.kafka.producer.OmniProducerFactory
import dev.owlmajin.flagforge.server.common.kafka.producer.sendAwait
import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.model.EnvironmentCommand
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaOperations
import org.springframework.stereotype.Component

interface EnvironmentRepository {
    suspend fun create(command: EnvironmentCommand)
}

@Component
class EnvironmentRepositoryImpl(
    private val persistenceProperties: PersistenceProperties,
    private val producerFactory: OmniProducerFactory,
) : EnvironmentRepository {

    companion object {
        private val log = LoggerFactory.getLogger(EnvironmentRepositoryImpl::class.java)
    }

    private val producer by lazy { producerFactory.createTopicProducer(persistenceProperties.flagCommands) }

    override suspend fun create(command: EnvironmentCommand) {
        producer.sendAwait(command.environmentId, command)

        log.debug("Environment command sent. type=${command::class.simpleName}, environmentId=${command.environmentId}, commandId=${command.id}")
    }
}
