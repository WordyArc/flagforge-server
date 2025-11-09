package dev.owlmajin.flagforge.server.persistence.repository

import dev.owlmajin.flagforge.server.common.kafka.producer.TopicProducerFactory
import dev.owlmajin.flagforge.server.common.kafka.producer.sendAwait
import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.model.EnvironmentCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

interface EnvironmentRepository {
    suspend fun create(command: EnvironmentCommand)
}

@Component
class EnvironmentRepositoryImpl(
    private val persistenceProperties: PersistenceProperties,
    private val producerFactory: TopicProducerFactory,
) : EnvironmentRepository {

    companion object {
        private val log = LoggerFactory.getLogger(EnvironmentRepositoryImpl::class.java)
    }

    private val producer by lazy {
        producerFactory.createProducer<String, EnvironmentCommand>(persistenceProperties.flagCommands)
    }

    override suspend fun create(command: EnvironmentCommand) {
        producer.sendAwait(command.environmentId, command)

        log.debug(
            "Environment command sent. type={}, environmentId={}, commandId={}",
            command::class.simpleName,
            command.environmentId,
            command.id,
        )
    }
}
