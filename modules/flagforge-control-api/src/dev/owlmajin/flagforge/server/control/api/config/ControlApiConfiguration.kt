package dev.owlmajin.flagforge.server.control.api.config

import dev.owlmajin.flagforge.server.common.CommonConfiguration
import dev.owlmajin.flagforge.server.common.kafka.config.CommonKafkaConfiguration
import dev.owlmajin.flagforge.server.persistence.config.PersistenceConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper

@Import(value = [
    CommonConfiguration::class,
    CommonKafkaConfiguration::class,
    PersistenceConfiguration::class,
])
@Profile("control-api")
@ComponentScan(basePackages = ["dev.owlmajin.flagforge.server.control.api"])
class ControlApiConfiguration {

    @Bean
    fun jacksonBuilder(): ObjectMapper =
        JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
            .build()

}