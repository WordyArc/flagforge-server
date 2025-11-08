package dev.owlmajin.flagforge.server.common.kafka.serde

import tools.jackson.databind.ObjectMapper

interface KafkaPayloadEncoder {
    fun <T: Any> encode(value: T): ByteArray
}

class JacksonPayloadEncoder(private val jsonMapper: ObjectMapper) : KafkaPayloadEncoder {
    override fun <T : Any> encode(value: T): ByteArray {
        return jsonMapper.writeValueAsBytes(value)
    }
}
