package dev.owlmajin.flagforge.server.processor.kafka.serde

import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory
import org.springframework.kafka.support.serializer.JacksonJsonSerde
import tools.jackson.databind.json.JsonMapper

class KafkaStreamsCommandDeserializer(jsonMapper: JsonMapper) : Deserializer<Any?> {

    private val log = LoggerFactory.getLogger(javaClass)

    private val delegate = JacksonJsonSerde(Any::class.java, jsonMapper).deserializer().apply {
        addTrustedPackages("dev.owlmajin.flagforge.server.model", "*")
        setUseTypeHeaders(true)
    }

    override fun configure(configs: Map<String, *>?, isKey: Boolean) {
        delegate.configure(configs.orEmpty().toMutableMap(), isKey)
    }

    override fun deserialize(topic: String, data: ByteArray?): Any? =
        deserialize(topic, null, data)

    override fun deserialize(topic: String, headers: Headers?, data: ByteArray?): Any? =
        try {
            if (data == null) {
                null
            } else {
                if (headers != null) {
                    val headersList = headers.toArray().map { h ->
                        "${h.key()}=${h.value()?.let { String(it) } ?: "null"}"
                    }
                    log.info("KafkaStreams deserializing from topic={}, headers={}", topic, headersList)
                } else {
                    log.warn("No headers for topic={}, deserialization may fail", topic)
                }

                val result = if (headers != null) {
                    delegate.deserialize(topic, headers, data)
                } else {
                    delegate.deserialize(topic, data)
                }

                log.info("Deserialized object: type={}, class={}", result?.let { it::class.simpleName }, result?.javaClass?.name)
                result
            }
        } catch (e: SerializationException) {
            log.error("Error deserializing record from topic={}, headers={}", topic, headers?.toArray()?.map { it.key() }, e)
            null
        } catch (e: Exception) {
            log.error("Unexpected error deserializing record from topic={}", topic, e)
            null
        }

    override fun close() {
        delegate.close()
    }
}
