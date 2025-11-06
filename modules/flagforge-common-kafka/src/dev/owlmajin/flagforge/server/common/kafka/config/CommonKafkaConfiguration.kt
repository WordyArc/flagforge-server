package dev.owlmajin.flagforge.server.common.kafka.config

import dev.owlmajin.flagforge.server.common.kafka.topic.KafkaConnect
import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(PersistenceProperties::class)
class CommonKafkaConfiguration {

    @Bean(destroyMethod = "close")
    fun kafkaConnect(persistenceProperties: PersistenceProperties) = KafkaConnect(persistenceProperties.kafka)
}