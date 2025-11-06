package dev.owlmajin.flagforge.server.common.kafka.producer

import dev.owlmajin.flagforge.server.common.kafka.topic.DATA_SCHEMA_HEADER_NAME
import dev.owlmajin.flagforge.server.common.kafka.topic.TopicProperties
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaOperations
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

class KafkaProducer<K: Any, V: Any>(
    private val topic: TopicProperties,
    private val kafkaOperations: KafkaOperations<K, V>,
) {
    fun send(key: K, value: V): CompletableFuture<SendResult<K, V>> = send(key, value, emptyMap())

    fun send(key: K, value: V, headers: Map<String, Any?>): CompletableFuture<SendResult<K, V>> {
        val record = ProducerRecord<K, V>(topic.name, key, value)

        topic.defaults
            ?.topicName
            ?.takeIf { it.isNotBlank() }
            ?.let { schema ->
                record.headers().add(DATA_SCHEMA_HEADER_NAME, schema.toByteArray())
            }

        headers.forEach { (name, headerValue) ->
            if (headerValue == null) return@forEach

            val bytes = when (headerValue) {
                is ByteArray -> headerValue
                else -> headerValue.toString().toByteArray()
            }
            record.headers().add(name, bytes)
        }

        return kafkaOperations.send(record)
    }
    
}
