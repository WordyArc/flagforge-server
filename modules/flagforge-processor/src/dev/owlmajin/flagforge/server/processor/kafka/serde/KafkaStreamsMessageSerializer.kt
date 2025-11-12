package dev.owlmajin.flagforge.server.processor.kafka.serde

import dev.owlmajin.flagforge.server.model.Message
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper

class KafkaStreamsMessageSerializer(
    private val jsonMapper: JsonMapper,
) : Serializer<Message<*>> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun serialize(topic: String, data: Message<*>?): ByteArray? =
        data?.let {
            try {
                jsonMapper.writeValueAsBytes(it)
            } catch (ex: Exception) {
                log.error("Failed to serialize message for topic={}", topic, ex)
                null
            }
        }

    override fun serialize(topic: String, headers: Headers?, data: Message<*>?): ByteArray? =
        serialize(topic, data)
}
