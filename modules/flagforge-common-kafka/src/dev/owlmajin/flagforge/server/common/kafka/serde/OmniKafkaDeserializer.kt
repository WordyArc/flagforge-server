package dev.owlmajin.flagforge.server.common.kafka.serde

import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer

class OmniKafkaDeserializer : Deserializer<Any?> {

    private val log = LoggerFactory.getLogger(javaClass)

    private val delegate = JacksonJsonDeserializer(Any::class.java).apply {
        val mapper = KafkaJacksonTypeMapper().apply {
            addTrustedPackages("dev.owlmajin.flagforge.server.model", "*")
        }
        setTypeMapper(mapper)
        setUseTypeHeaders(true)
    }

    override fun configure(configs: Map<String, *>?, isKey: Boolean) {
        configs?.let { delegate.configure(it, isKey) }
            ?: error("could not configure configs")
    }

    override fun deserialize(topic: String, data: ByteArray): Any? =
        try {
            delegate.deserialize(topic, data)
        } catch (e: SerializationException) {
            log.debug("Error deserializing record from topic=$topic", e)
            null
        }

    override fun close() {
        delegate.close()
    }
}