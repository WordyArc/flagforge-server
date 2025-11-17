package dev.owlmajin.flagforge.server.persistence.repository

import dev.owlmajin.flagforge.server.common.kafka.producer.OmniProducerFactory
import dev.owlmajin.flagforge.server.common.kafka.producer.sendAwait
import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.environment.CreateEnvironmentCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

interface EnvironmentRepository {
    suspend fun create(command: CommandMessage<CreateEnvironmentCommand>)
}

@Component
class EnvironmentRepositoryImpl(
    private val persistenceProperties: PersistenceProperties,
    private val producerFactory: OmniProducerFactory,
) : EnvironmentRepository {

    companion object {
        private val log = LoggerFactory.getLogger(EnvironmentRepositoryImpl::class.java)
    }

    private val producer by lazy { producerFactory.createTopicProducer(persistenceProperties.commandMessages) }

    override suspend fun create(command: CommandMessage<CreateEnvironmentCommand>) {
        val payload = command.payload
        producer.sendAwait(payload.environmentId, command)

        log.debug(
            "Environment command sent. type={}, environmentId={}, commandId={}",
            payload::class.simpleName,
            payload.environmentId,
            command.header.id,
        )
    }
}
