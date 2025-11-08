package dev.owlmajin.flagforge.server.common.kafka.producer

import dev.owlmajin.flagforge.server.common.kafka.topic.TopicProperties
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import org.springframework.kafka.core.KafkaOperations
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

class KafkaTopicProducer<K: Any, V: Any>(
    internal val topic: TopicProperties,
    private val kafkaOperations: KafkaOperations<K, V>,
) {
    fun send(key: K, value: V): CompletableFuture<SendResult<K, V>> = send(key, value, topic.defaultHeaders())

    fun send(key: K, value: V, headers: List<Header>,): CompletableFuture<SendResult<K, V>> {
        val record = ProducerRecord<K, V>(topic.effectiveName, null, null, key, value, headers)

        return kafkaOperations.send(record)
    }

    fun send(
        key: K,
        value: V,
        extraHeaders: Map<String, Any?>,
    ): CompletableFuture<SendResult<K, V>> {
        val headers = topic.defaultHeaders().apply {
            extraHeaders.forEach { (name, headerValue) ->
                if (headerValue == null) return@forEach

                val bytes = when (headerValue) {
                    is ByteArray -> headerValue
                    else -> headerValue.toString().toByteArray()
                }
                add(RecordHeader(name, bytes))
            }
        }
        return send(key, value, headers)
    }
    
}
