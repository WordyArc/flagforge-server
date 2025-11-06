package dev.owlmajin.flagforge.server.common.kafka

import org.slf4j.LoggerFactory
import org.springframework.boot.kafka.autoconfigure.KafkaProperties


private const val CLEANUP_POLICY = "cleanup.policy"

data class TopicProperties(
    var name: String = "",
    var concurrency: Int = 1,
    var partitions: Int = 1,
    var replicationFactor: Int = 1,
    var producer: KafkaProperties.Producer? = null,
    var consumer: KafkaProperties.Consumer? = null,
    var topicConfig: Map<String, String> = emptyMap(),
    var metadata: TopicMetadata? = null,
) {

    private val log = LoggerFactory.getLogger(javaClass)

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

    fun overrideWith(defaults: TopicProperties): TopicProperties = copyWith(metadata ?: defaults.metadata)

    private fun copyWith(defaultConfig: TopicMetadata?): TopicProperties = copy(metadata = defaultConfig)

    fun applyMetadataDefaults(): TopicProperties {
        val metadata = metadata ?: return this

        if (!topicConfig.containsKey(CLEANUP_POLICY)) {
            topicConfig = topicConfig + (CLEANUP_POLICY to metadata.cleanupPolicy)
            log.debug("Set default $CLEANUP_POLICY=${metadata.cleanupPolicy} for topic=${effectiveName}")
        }
        return this
    }

    private val effectiveName: String
        get() = name.ifBlank { metadata?.topicName ?: "<unnamed>" }

}

data class TopicMetadata(
    val topicName: String,
    val description: String = "",
    val cleanupPolicy: String,
    val header: String,
)
