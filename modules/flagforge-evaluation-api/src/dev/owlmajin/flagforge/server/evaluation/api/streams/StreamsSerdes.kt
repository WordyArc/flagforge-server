package dev.owlmajin.flagforge.server.evaluation.api.streams

import dev.owlmajin.flagforge.server.common.kafka.serde.KafkaJacksonTypeMapper
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import org.springframework.kafka.support.serializer.JacksonJsonSerializer
import tools.jackson.databind.json.JsonMapper


object StreamsSerdes {

    fun <T : Any> json(valueType: Class<T>, mapper: JsonMapper): Serde<T> {
        val typeMapper = KafkaJacksonTypeMapper().apply {
            addTrustedPackages("dev.owlmajin.flagforge.server.model", "*")
        }

        val serializer = JacksonJsonSerializer<T>(mapper).apply {
            setTypeMapper(typeMapper)
            setAddTypeInfo(true)
        }
        val deserializer = JacksonJsonDeserializer(valueType, mapper).apply {
            setTypeMapper(typeMapper)
            setUseTypeHeaders(true)
        }

        return Serdes.serdeFrom(
            serializer as Serializer<T>,
            deserializer as Deserializer<T>,
        )
    }

    inline fun <reified T : Any> json(mapper: JsonMapper) = json(T::class.java, mapper)
}
