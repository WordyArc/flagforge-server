package dev.owlmajin.flagforge.server.evaluation.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jsonMapper

@Profile("evaluation-api")
@Configuration
@ComponentScan("dev.owlmajin.flagforge.server.evaluation.api")
class EvaluationApiConfiguration {

    @Bean
    fun jacksonBuilder(): ObjectMapper =
        jsonMapper {
            enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
        }

}
