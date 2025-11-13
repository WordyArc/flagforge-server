package dev.owlmajin.flagforge.server.common.streams.serde

import dev.owlmajin.flagforge.server.common.kafka.serde.KafkaJacksonTypeMapper
import dev.owlmajin.flagforge.server.model.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.serialization.Deserializer
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import tools.jackson.databind.json.JsonMapper

class KafkaStreamsDeserializer(jsonMapper: JsonMapper) : Deserializer<Message<*>> {

    private val klog = KotlinLogging.logger { javaClass }

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
            klog.error(ex) { "Failed to deserialize record for topic=$topic due to serialization issue" }
            null
        } catch (ex: Exception) {
            klog.error(ex) { "Unexpected error while deserializing record for topic=$topic" }
            null
        }
    }
}
