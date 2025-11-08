package dev.owlmajin.flagforge.server.persistence.repository

import com.sun.org.slf4j.internal.LoggerFactory
import dev.owlmajin.flagforge.server.common.kafka.producer.KafkaProducerFactory
import dev.owlmajin.flagforge.server.common.kafka.producer.sendAwait
import dev.owlmajin.flagforge.server.common.kafka.serde.KafkaPayloadEncoder
import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.model.FlagCommand
import org.springframework.stereotype.Component

interface FlagCommandRepository {
    suspend fun send(command: FlagCommand)
}

@Component
class FlagCommandRepositoryImpl(
    private val persistenceProperties: PersistenceProperties,
    private val producerFactory: KafkaProducerFactory,
    private val encoder: KafkaPayloadEncoder,
) : FlagCommandRepository {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    private val producer by lazy { producerFactory.createProducer<String, ByteArray>(persistenceProperties.flagCommands) }

    override suspend fun send(command: FlagCommand) {
        val key = command.flagId.value.toString()
        val payload = encoder.encode(command)

        producer.sendAwait(key, payload)

        log.debug("Flag command sen. type=${command::class.simpleName}, flagId=${command.flagId}, commandId=${command.commandId}")
    }
}