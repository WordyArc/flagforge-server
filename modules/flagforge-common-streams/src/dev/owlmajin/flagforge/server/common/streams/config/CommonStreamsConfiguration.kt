package dev.owlmajin.flagforge.server.common.streams.config

import dev.owlmajin.flagforge.server.common.kafka.config.CommonKafkaConfiguration
import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.common.streams.serde.KafkaStreamsDeserializer
import dev.owlmajin.flagforge.server.common.streams.serde.KafkaStreamsSerializer
import dev.owlmajin.flagforge.server.model.FlagState
import dev.owlmajin.flagforge.server.model.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.streams.StreamsConfig
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.support.serializer.JacksonJsonSerde
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.kotlinModule

@Import(CommonKafkaConfiguration::class)
@Configuration
@EnableKafkaStreams
@ComponentScan("dev.owlmajin.flagforge.server.common.streams")
class CommonStreamsConfiguration {

    private val klog = KotlinLogging.logger { javaClass }
    @Bean
    fun createKotlinModule() = kotlinModule {
        enable(KotlinFeature.StrictNullChecks)
    }

    @Bean
    fun jsonMapper(createKotlinModule: KotlinModule): JsonMapper = tools.jackson.module.kotlin.jsonMapper {
        addModule(createKotlinModule)
    }

    @Bean(name = [KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME])
    fun kafkaStreamsConfig(persistenceProperties: PersistenceProperties): KafkaStreamsConfiguration {
        val kafka: KafkaProperties = persistenceProperties.kafka
        val props = kafka.buildStreamsProperties().toMutableMap()

        val appId = kafka.streams.applicationId ?: "flagforge-processor"
        props[StreamsConfig.APPLICATION_ID_CONFIG] = appId

        val guarantee = kafka.streams.properties["processing.guarantee"] ?: StreamsConfig.EXACTLY_ONCE_V2
        props[StreamsConfig.PROCESSING_GUARANTEE_CONFIG] = guarantee

        props.putIfAbsent(StreamsConfig.STATE_DIR_CONFIG, "data/streams/flagforge-processor")

        klog.info { "Starting Kafka Streams application with $appId and guarantee=$guarantee" }
        return KafkaStreamsConfiguration(props)
    }

    @Bean
    fun messageSerde(jsonMapper: JsonMapper): Serde<Message<*>> =
        Serdes.serdeFrom(
            KafkaStreamsSerializer(jsonMapper) as Serializer<Message<*>>,
            KafkaStreamsDeserializer(jsonMapper) as Deserializer<Message<*>>,
        )

    @Bean
    fun flagStateSerde(jsonMapper: JsonMapper): JacksonJsonSerde<FlagState> {
        return JacksonJsonSerde(FlagState::class.java, jsonMapper)
    }

}