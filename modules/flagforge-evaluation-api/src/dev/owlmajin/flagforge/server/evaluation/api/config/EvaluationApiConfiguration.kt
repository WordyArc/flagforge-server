package dev.owlmajin.flagforge.server.evaluation.api.config

import dev.owlmajin.flagforge.server.common.CommonConfiguration
import dev.owlmajin.flagforge.server.common.kafka.config.CommonKafkaConfiguration
import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.evaluation.api.streams.StreamsSerdes
import dev.owlmajin.flagforge.server.model.FlagState
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.StreamsConfig
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import org.springframework.kafka.streams.KafkaStreamsInteractiveQueryService
import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule


@Import(
    CommonConfiguration::class,
    CommonKafkaConfiguration::class,
)
@Profile("evaluation-api")
@Configuration
@ComponentScan("dev.owlmajin.flagforge.server.evaluation.api")
@EnableKafkaStreams
class EvaluationApiConfiguration {


    private val log = KotlinLogging.logger { javaClass }

    @Bean
    fun kotlinModule(): KotlinModule = kotlinModule {
        enable(KotlinFeature.StrictNullChecks)
    }

    @Bean
    fun jsonMapper(kotlinModule: KotlinModule): ObjectMapper =
        jsonMapper {
            addModule(kotlinModule)
            enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
        }

    @Bean
    fun interactiveQueryService(streamsBuilderFactoryBean: StreamsBuilderFactoryBean) =
        KafkaStreamsInteractiveQueryService(streamsBuilderFactoryBean)

    @DependsOn("topicInitializer")
    @Bean(name = [KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME])
    fun kafkaStreamsConfig(persistenceProperties: PersistenceProperties): KafkaStreamsConfiguration {
        val kafka: KafkaProperties = persistenceProperties.kafka
        val props = kafka.buildStreamsProperties().toMutableMap()

        val appId = kafka.streams.applicationId ?: "flagforge-eval"
        props[StreamsConfig.APPLICATION_ID_CONFIG] = appId

        val guarantee = kafka.streams.properties["processing.guarantee"] ?: StreamsConfig.AT_LEAST_ONCE
        props[StreamsConfig.PROCESSING_GUARANTEE_CONFIG] = guarantee

        props.putIfAbsent(StreamsConfig.STATE_DIR_CONFIG, "data/streams/flagforge-eval")

        log.info { "Starting Evaluation Kafka Streams app with appId=$appId, guarantee=$guarantee" }
        return KafkaStreamsConfiguration(props)
    }

    @Bean
    fun flagStateSerde(jsonMapper: JsonMapper): Serde<FlagState> =
        StreamsSerdes.json(jsonMapper)

}
