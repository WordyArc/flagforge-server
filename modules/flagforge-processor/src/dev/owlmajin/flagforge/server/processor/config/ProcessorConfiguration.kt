package dev.owlmajin.flagforge.server.processor.config

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.owlmajin.flagforge.server.common.kafka.config.CommonKafkaConfiguration
import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.model.FlagAggregate
import dev.owlmajin.flagforge.server.processor.kafka.serde.KafkaStreamsCommandDeserializer
import dev.owlmajin.flagforge.server.processor.kafka.serde.KafkaStreamsCommandSerializer
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.streams.StreamsConfig
import org.slf4j.LoggerFactory
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.support.serializer.JsonSerde as SpringKafkaJsonSerde

@Profile("processor")
@Import(CommonKafkaConfiguration::class)
@Configuration
@EnableKafkaStreams
@ComponentScan(basePackages = ["dev.owlmajin.flagforge.server.processor"])
class ProcessorConfiguration() {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean(name = [KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME])
    fun kafkaStreamsConfig(persistenceProperties: PersistenceProperties): KafkaStreamsConfiguration {
        val kafka: KafkaProperties = persistenceProperties.kafka
        val props = kafka.buildStreamsProperties().toMutableMap()

        val appId = kafka.streams.applicationId ?: "flagforge-processor"
        props[StreamsConfig.APPLICATION_ID_CONFIG] = appId

        val guarantee = kafka.streams.properties["processing.guarantee"] ?: StreamsConfig.EXACTLY_ONCE_V2
        props[StreamsConfig.PROCESSING_GUARANTEE_CONFIG] = guarantee

        props.putIfAbsent(StreamsConfig.STATE_DIR_CONFIG, "data/streams/flagforge-processor")

        log.info("Starting Kafka Streams application with $appId and guarantee=$guarantee")
        return KafkaStreamsConfiguration(props)
    }

    @Bean
    fun commandSerde(): Serde<Any> =
        Serdes.serdeFrom(
            KafkaStreamsCommandSerializer() as Serializer<Any>,
            KafkaStreamsCommandDeserializer() as Deserializer<Any?>
        )

    @Bean
    fun flagAggregateSerde(): SpringKafkaJsonSerde<FlagAggregate> {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
            .registerKotlinModule()
            .findAndRegisterModules()
        
        return SpringKafkaJsonSerde(FlagAggregate::class.java, objectMapper)
    }

    @Bean
    fun flagEventSerde(): SpringKafkaJsonSerde<Any> {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
            .registerKotlinModule()
            .findAndRegisterModules()
        
        return SpringKafkaJsonSerde(Any::class.java, objectMapper).apply {
            deserializer().addTrustedPackages("dev.owlmajin.flagforge.server.model", "*")
        }
    }

}
