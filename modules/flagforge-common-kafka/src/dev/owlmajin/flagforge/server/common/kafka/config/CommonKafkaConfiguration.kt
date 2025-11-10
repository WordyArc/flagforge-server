package dev.owlmajin.flagforge.server.common.kafka.config

import dev.owlmajin.flagforge.server.common.kafka.producer.TopicProducerFactory
import dev.owlmajin.flagforge.server.common.kafka.topic.KafkaConnect
import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(PersistenceProperties::class)
@ComponentScan("dev.owlmajin.flagforge.server.common.kafka")
class CommonKafkaConfiguration {

    @Bean(destroyMethod = "close")
    fun kafkaConnect(persistenceProperties: PersistenceProperties) = KafkaConnect(persistenceProperties.kafka)


    @Bean(destroyMethod = "close")
    fun topicProducerFactory(kafkaProperties: KafkaProperties) = TopicProducerFactory(kafkaProperties)
}
