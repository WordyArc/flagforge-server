package dev.owlmajin.flagforge.server.common.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.kafka.autoconfigure.KafkaProperties

@ConfigurationProperties("app.persistence")
data class PersistenceProperties(
    val enabled: Boolean = true,
    val autoCreateTopics: Boolean = true,
    val kafka: KafkaProperties = KafkaProperties(),
    val topics: Map<String, TopicProperties> = emptyMap(),
)