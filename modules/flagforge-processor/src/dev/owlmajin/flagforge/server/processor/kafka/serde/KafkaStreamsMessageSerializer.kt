package dev.owlmajin.flagforge.server.processor.kafka.serde

import dev.owlmajin.flagforge.server.common.kafka.serde.KafkaJacksonTypeMapper
import dev.owlmajin.flagforge.server.model.Message
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import org.springframework.kafka.support.serializer.JacksonJsonSerializer
import tools.jackson.databind.json.JsonMapper

class KafkaStreamsMessageSerializer(jsonMapper: JsonMapper) : Serializer<Message<*>> {

    private val log = LoggerFactory.getLogger(javaClass)

    /*private val delegate = JacksonJsonSerde<Any>(jsonMapper).serializer().apply {
        setAddTypeInfo(true)
    }*/

    private val delegate = JacksonJsonSerializer<Message<*>>(jsonMapper).apply {
        val mapper = KafkaJacksonTypeMapper().apply {
            addTrustedPackages("dev.owlmajin.flagforge.server.model", "*")
        }
        setTypeMapper(mapper)
        setAddTypeInfo(true)
    }

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {
        configs?.let { delegate.configure(it, isKey) }
    }

    override fun serialize(topic: String, data: Message<*>?): ByteArray? =
        data?.let {
            try {
                delegate.serialize(topic, it)
            } catch (ex: Exception) {
                log.error("Failed to serialize message for topic={}", topic, ex)
                null
            }
        }

    override fun serialize(topic: String, headers: Headers, data: Message<*>?): ByteArray? =
        data?.let {
            try {
                delegate.serialize(topic, headers, it)
            } catch (ex: Exception) {
                log.error("Failed to serialize message for topic={}", topic, ex)
                null
            }
        }

    override fun close() {
        delegate.close()
    }
}
