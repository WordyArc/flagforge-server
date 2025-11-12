package dev.owlmajin.flagforge.server.processor.kafka.serde

import dev.owlmajin.flagforge.server.common.kafka.serde.KafkaJacksonTypeMapper
import dev.owlmajin.flagforge.server.model.Message
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import tools.jackson.databind.json.JsonMapper

class KafkaStreamsMessageDeserializer(jsonMapper: JsonMapper) : Deserializer<Message<*>> {

    private val log = LoggerFactory.getLogger(javaClass)

    private val delegate = JacksonJsonDeserializer(Message::class.java, jsonMapper).apply {
        val mapper = KafkaJacksonTypeMapper().apply {
            addTrustedPackages("dev.owlmajin.flagforge.server.model", "*")
        }
        setTypeMapper(mapper)
        setUseTypeHeaders(true)
    }

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {
        configs?.let { delegate.configure(it, isKey) }
    }

    override fun deserialize(topic: String, data: ByteArray?): Message<*>? =
        deserialize(topic, RecordHeaders(), data)

    override fun deserialize(topic: String, headers: Headers, data: ByteArray?): Message<*>? {
        if (data == null) {
            return null
        }

        return try {
            delegate.deserialize(topic, headers, data)
        } catch (ex: SerializationException) {
            log.error("Failed to deserialize record for topic={} due to serialization issue", topic, ex)
            null
        } catch (ex: Exception) {
            log.error("Unexpected error while deserializing record for topic={}", topic, ex)
            null
        }
    }
}
