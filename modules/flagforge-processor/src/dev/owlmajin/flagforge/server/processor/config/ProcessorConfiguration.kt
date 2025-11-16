package dev.owlmajin.flagforge.server.processor.config

import dev.owlmajin.flagforge.server.common.kafka.config.CommonKafkaConfiguration
import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.model.FlagState
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.processor.serde.StreamsSerdes
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.StreamsConfig
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration
import org.springframework.kafka.config.KafkaStreamsConfiguration
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.kotlinModule

@Profile("processor")
@Import(CommonKafkaConfiguration::class)
@Configuration
@ComponentScan(basePackages = ["dev.owlmajin.flagforge.server.processor"])
class ProcessorConfiguration() {

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
        StreamsSerdes.json<Message<*>>(jsonMapper)

    @Bean
    fun flagStateSerde(jsonMapper: JsonMapper): Serde<FlagState> =
        StreamsSerdes.json<FlagState>(jsonMapper)

    @Bean
    fun anySerde(jsonMapper: JsonMapper): Serde<Any> =
        StreamsSerdes.json<Any>(jsonMapper)
}
