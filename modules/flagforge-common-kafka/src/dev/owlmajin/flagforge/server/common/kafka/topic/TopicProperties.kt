package dev.owlmajin.flagforge.server.common.kafka.topic

import org.slf4j.LoggerFactory
import org.springframework.boot.kafka.autoconfigure.KafkaProperties


private const val CLEANUP_POLICY = "cleanup.policy"

data class TopicProperties(
    var name: String = "",
    var concurrency: Int = 1,
    var partitions: Int = 1,
    var replicationFactor: Short = 1,
    var producer: KafkaProperties.Producer? = null,
    var consumer: KafkaProperties.Consumer? = null,
    var topicConfig: Map<String, String> = emptyMap(),
    var header: String = "",
    var defaults: TopicDefaults? = null,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun withDefaults(
        topicName: String,
        description: String,
        cleanupPolicy: String,
        header: String,
    ): TopicProperties = copy(
        name = name.ifBlank { topicName },
        defaults = TopicDefaults(
            topicName = topicName,
            description = description,
            cleanupPolicy = cleanupPolicy,
            header = header,
        )
    )

    fun overrideWith(other: TopicProperties): TopicProperties = other.copyWith(defaults ?: other.defaults)

    private fun copyWith(defaultConfig: TopicDefaults?): TopicProperties = copy(defaults = defaultConfig)

    fun applyDefaults(): TopicProperties {
        val topicDefaults = defaults ?: return this

        header = topicDefaults.header

        if (!topicConfig.containsKey(CLEANUP_POLICY)) {
            topicConfig = topicConfig + (CLEANUP_POLICY to topicDefaults.cleanupPolicy)
            log.debug("Set default $CLEANUP_POLICY=${topicDefaults.cleanupPolicy} for topic=${effectiveName}")
        }
        return this
    }

    internal val effectiveName: String
        get() = name.ifBlank { defaults?.topicName ?: "<unnamed>" }

}

data class TopicDefaults(
    val topicName: String,
    val description: String = "",
    val cleanupPolicy: String,
    val header: String,
)
