package dev.owlmajin.flagforge.server.common.kafka.serde

import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.serialization.UUIDSerializer
import org.springframework.kafka.support.serializer.JacksonJsonSerializer
import java.util.UUID


class OmniKafkaSerializer(val config: Map<String, Any> = emptyMap()) : Serializer<Any?> {

    private val stringSerializer = StringSerializer()
    private val uuidSerializer = UUIDSerializer()
    private val byteArraySerializer = ByteArraySerializer()
    private val objectSerializer = JacksonJsonSerializer<Any>().apply {
        val mapper = KafkaJacksonTypeMapper().apply {
            addTrustedPackages("dev.owlmajin.flagforge.server.model", "*")
        }
        typeMapper = mapper
        isAddTypeInfo = true
    }

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {
        val effectiveConfig: Map<String, *> = config.takeIf { it.isNotEmpty() } ?: configs.orEmpty()

        stringSerializer.configure(effectiveConfig, isKey)
        uuidSerializer.configure(effectiveConfig, isKey)
        byteArraySerializer.configure(effectiveConfig, isKey)
        objectSerializer.configure(effectiveConfig, isKey)
    }

    override fun serialize(topic: String, data: Any?): ByteArray? = serialize(topic, null, data)

    override fun serialize(topic: String, headers: Headers?, data: Any?): ByteArray? =
        when (data) {
            null -> null
            is ByteArray -> byteArraySerializer.serialize(topic, headers, data)
            is String -> stringSerializer.serialize(topic, headers, data)
            is UUID -> uuidSerializer.serialize(topic, headers, data)
            else -> serializeObject(topic, headers, data)
        }

    override fun close() {
        stringSerializer.close()
        uuidSerializer.close()
        byteArraySerializer.close()
        objectSerializer.close()
    }

    // ToDo fix!!
    private fun serializeObject(topic: String, headers: Headers?, data: Any?): ByteArray {
        println("=== OmniKafkaSerializer ===")
        println("Serializing object: topic=$topic, type=${data?.javaClass?.name}")
        println("Headers present: ${headers != null}")
        
        val result = if (headers != null) {
            objectSerializer.serialize(topic, headers, data)
        } else {
            objectSerializer.serialize(topic, data)
        }
        
        if (headers != null) {
            val headersList = headers.toArray().map { h -> 
                "${h.key()}=${h.value()?.let { String(it) } ?: "null"}" 
            }
            println("Headers after serialization: $headersList")
        }
        println("===========================")
        
        return result
    }
}
