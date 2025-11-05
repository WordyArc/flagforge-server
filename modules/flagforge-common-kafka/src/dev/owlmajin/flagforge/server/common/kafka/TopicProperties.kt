package dev.owlmajin.flagforge.server.common.kafka

import org.springframework.boot.context.properties.bind.ConstructorBinding
import org.springframework.boot.kafka.autoconfigure.KafkaProperties


data class TopicProperties @ConstructorBinding constructor(
    val name: String = "",
    val concurrency: Int = 1,
    val partitions: Int = 1,
    val replicationFactor: Int = 1,
    val producer: KafkaProperties.Producer? = null,
    val consumer: KafkaProperties.Consumer? = null,
    val topicConfig: Map<String, String> = emptyMap(),
    val defaultConfig: DefaultConfig? = null,
) {

    fun withDefault(
        topicName: String,
        description: String,
        cleanupPolicy: String,
        header: String,
    ): TopicProperties = withDefault(
        DefaultConfig(
            topicName = topicName,
            description = description,
            cleanupPolicy = cleanupPolicy,
            header = header,
        )
    )

    fun withDefault(topic: TopicProperties): TopicProperties = withDefault(topic.defaultConfig)

    private fun withDefault(defaultConfig: DefaultConfig?): TopicProperties = copy(defaultConfig = defaultConfig)

}

data class DefaultConfig(
    val topicName: String,
    val description: String = "",
    val cleanupPolicy: String,
    val header: String,
)
