package dev.owlmajin.flagforge.server.processor.kafka.serde

import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.model.MessagePayload
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper

class KafkaStreamsMessageDeserializer(
    private val jsonMapper: JsonMapper,
) : Deserializer<Message<*>> {

    private val log = LoggerFactory.getLogger(javaClass)
    private val typeRef = object : TypeReference<Message<MessagePayload>>() {}

    override fun deserialize(topic: String, data: ByteArray?): Message<*>? =
        deserialize(topic, null, data)

    override fun deserialize(topic: String, headers: Headers?, data: ByteArray?): Message<*>? {
        if (data == null) {
            return null
        }

        return try {
            jsonMapper.readValue(data, typeRef)
        } catch (ex: SerializationException) {
            log.error("Failed to deserialize record for topic={} due to serialization issue", topic, ex)
            null
        } catch (ex: Exception) {
            log.error("Unexpected error while deserializing record for topic={}", topic, ex)
            null
        }
    }
}
