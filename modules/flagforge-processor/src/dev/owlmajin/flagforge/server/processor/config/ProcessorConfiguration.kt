package dev.owlmajin.flagforge.server.processor.config

import dev.owlmajin.flagforge.server.common.kafka.config.CommonKafkaConfiguration
import dev.owlmajin.flagforge.server.common.kafka.serde.KafkaJacksonTypeMapper
import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.model.FlagAggregate
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
import org.springframework.kafka.support.serializer.JacksonJsonSerde
import tools.jackson.databind.json.JsonMapper

@Profile("processor")
@Import(CommonKafkaConfiguration::class)
@Configuration
@EnableKafkaStreams
@ComponentScan(basePackages = ["dev.owlmajin.flagforge.server.processor"])
class ProcessorConfiguration() {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun jsonMapper(): JsonMapper =
        JsonMapper.builder()
            .findAndAddModules()
            .build()

    @Bean(name = [KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME])
    fun kafkaStreamsConfig(persistenceProperties: PersistenceProperties): KafkaStreamsConfiguration {
        val kafka: KafkaProperties = persistenceProperties.kafka
        val props = kafka.buildStreamsProperties().toMutableMap()

        val appId = kafka.streams.applicationId ?: "flagforge-processor"
        props[StreamsConfig.APPLICATION_ID_CONFIG] = appId

        val guarantee = kafka.streams.properties["processing.guarantee"] ?: StreamsConfig.EXACTLY_ONCE_V2
        props[StreamsConfig.PROCESSING_GUARANTEE_CONFIG] = guarantee

        log.info("Starting Kafka Streams application with $appId and guarantee=$guarantee")
        return KafkaStreamsConfiguration(props)
    }

    @Bean
    fun commandSerde(): JacksonJsonSerde<Any> =
        JacksonJsonSerde(Any::class.java).apply {
            deserializer().typeMapper = KafkaJacksonTypeMapper().apply {
                addTrustedPackages("dev.owlmajin.flagforge.server.model", "*")
            }
        }

    @Bean
    fun flagAggregateSerde(mapper: JsonMapper): JacksonJsonSerde<FlagAggregate> =
        JacksonJsonSerde(FlagAggregate::class.java, mapper)

    @Bean
    fun flagEventSerde(mapper: JsonMapper): JacksonJsonSerde<Any> =
        JacksonJsonSerde(Any::class.java, mapper).apply {
            deserializer().typeMapper = KafkaJacksonTypeMapper().apply {
                addTrustedPackages("dev.owlmajin.flagforge.server.model", "*")
            }
        }

}
