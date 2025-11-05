package dev.owlmajin.flagforge.server.common.kafka

import org.springframework.boot.kafka.autoconfigure.KafkaProperties


data class TopicProperties (
    var name: String = "",
    var concurrency: Int = 1,
    var partitions: Int = 1,
    var replicationFactor: Int = 1,
    var producer: KafkaProperties.Producer? = null,
    var consumer: KafkaProperties.Consumer? = null,
    var topicConfig: Map<String, String> = emptyMap(),
    var metadata: TopicMetadata? = null,
) {

    fun withMetadata(
        topicName: String,
        description: String,
        cleanupPolicy: String,
        header: String,
    ): TopicProperties = copy(
        name = name.ifBlank { topicName },
        metadata = TopicMetadata(
            topicName = topicName,
            description = description,
            cleanupPolicy = cleanupPolicy,
            header = header,
        )
    )

    fun overrideWith(defaults: TopicProperties): TopicProperties = copyWith(defaults.metadata)

    private fun copyWith(defaultConfig: TopicMetadata?): TopicProperties = copy(metadata = defaultConfig)
}

data class TopicMetadata(
    val topicName: String,
    val description: String = "",
    val cleanupPolicy: String,
    val header: String,
)
