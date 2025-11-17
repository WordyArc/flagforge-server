package dev.owlmajin.flagforge.server.persistence.repository

import dev.owlmajin.flagforge.server.common.kafka.producer.OmniProducerFactory
import dev.owlmajin.flagforge.server.common.kafka.producer.sendAwait
import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.flag.CreateFlagCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

interface FlagRepository {
    suspend fun create(command: CommandMessage<CreateFlagCommand>)
}

@Component
class FlagRepositoryImpl(
    private val persistenceProperties: PersistenceProperties,
    private val producerFactory: OmniProducerFactory,
) : FlagRepository {

    companion object {
        private val log = LoggerFactory.getLogger(FlagRepositoryImpl::class.java)
    }

    private val producer by lazy { producerFactory.createTopicProducer(persistenceProperties.commandMessages) }

    override suspend fun create(command: CommandMessage<CreateFlagCommand>) {
        val payload = command.payload
        producer.sendAwait(payload.flagId, command)

        log.debug(
            "Flag command sent. type={}, flagId={}, commandId={}",
            payload::class.simpleName,
            payload.flagId,
            command.header.id,
        )
    }
}
