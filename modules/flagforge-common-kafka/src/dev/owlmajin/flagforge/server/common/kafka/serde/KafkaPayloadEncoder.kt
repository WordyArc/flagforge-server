package dev.owlmajin.flagforge.server.common.kafka.serde

import tools.jackson.databind.json.JsonMapper

interface KafkaPayloadEncoder {
    fun <T: Any> encode(value: T): ByteArray
}

class JacksonPayloadEncoder(private val jsonMapper: JsonMapper) : KafkaPayloadEncoder {
    override fun <T : Any> encode(value: T): ByteArray {
        return jsonMapper.writeValueAsBytes(value)
    }
}
