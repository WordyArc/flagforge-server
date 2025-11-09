package dev.owlmajin.flagforge.server.persistence.repository

import dev.owlmajin.flagforge.server.common.kafka.producer.TopicProducerFactory
import dev.owlmajin.flagforge.server.common.kafka.producer.sendAwait
import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.model.FlagCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

interface FlagRepository {
    suspend fun create(command: FlagCommand)
}

@Component
class FlagRepositoryImpl(
    private val persistenceProperties: PersistenceProperties,
    private val producerFactory: TopicProducerFactory,
) : FlagRepository {

    companion object {
        private val log = LoggerFactory.getLogger(FlagRepositoryImpl::class.java)
    }

    private val producer by lazy { producerFactory.createProducer<String, FlagCommand>(persistenceProperties.flagCommands) }

    override suspend fun create(command: FlagCommand) {
        val key = command.flagId

        producer.sendAwait(key, command)

        log.debug("Flag command sent. type=${command::class.simpleName}, flagId=${command.flagId}, commandId=${command.id}")
    }
}
