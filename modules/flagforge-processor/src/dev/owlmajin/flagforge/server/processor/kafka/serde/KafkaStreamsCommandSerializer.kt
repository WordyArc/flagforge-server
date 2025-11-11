package dev.owlmajin.flagforge.server.processor.kafka.serde

import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Serializer
import org.springframework.kafka.support.serializer.JacksonJsonSerde
import tools.jackson.databind.json.JsonMapper

class KafkaStreamsCommandSerializer(jsonMapper: JsonMapper) : Serializer<Any?> {

    private val delegate = JacksonJsonSerde<Any>(jsonMapper).serializer().apply {
        isAddTypeInfo = true
    }

    override fun configure(configs: Map<String, *>?, isKey: Boolean) {
        delegate.configure(configs.orEmpty().toMutableMap(), isKey)
    }

    override fun serialize(topic: String, data: Any?): ByteArray? =
        serialize(topic, null, data)

    override fun serialize(topic: String, headers: Headers?, data: Any?): ByteArray? {
        println("=== KafkaStreamsCommandSerializer ===")
        println("Serializing: topic=$topic, type=${data?.javaClass?.name}")
        println("Headers present: ${headers != null}")

        val result = if (headers != null) {
            delegate.serialize(topic, headers, data)
        } else {
            delegate.serialize(topic, data)
        }

        if (headers != null) {
            val headersList = headers.toArray().map { h ->
                "${h.key()}=${h.value()?.let { String(it) } ?: "null"}"
            }
            println("Headers after serialization: $headersList")
        }
        println("===================================")

        return result
    }

    override fun close() {
        delegate.close()
    }
}
