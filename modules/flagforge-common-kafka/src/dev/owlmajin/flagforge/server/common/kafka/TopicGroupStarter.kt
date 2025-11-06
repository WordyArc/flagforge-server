package dev.owlmajin.flagforge.server.common.kafka

class TopicGroupStarter(
    val groupName: String,
    val kafkaConnect: KafkaConnect,
    val autoCreateTopicNames: Set<String>,
    val topics: Map<String, TopicProperties>,
    val shouldValidate: Boolean,
) {

    init {
        topics.values.forEach { it.applyMetadataDefaults() }
        if (shouldValidate) {
            createTopicsIfNeeded()
            validateTopicsIfNeeded()
        }
    }

    fun isAnyEnabled(): Boolean = topics.isNotEmpty()

    internal fun bootstrapServers() =
        kafkaConnect.kafkaAdmin.configurationProperties["bootstrap.servers"]?.toString() ?: "<unknown>"

    companion object {
        fun ofTopics(
            groupName: String,
            kafkaConnect: KafkaConnect,
            enabled: Boolean,
            autoCreateTopics: Boolean,
            topics: Set<TopicProperties>,
        ): TopicGroupStarter {
            val preparedTopics =
                if (enabled) topics.associateBy { it.name }
                else emptyMap()


            return TopicGroupStarter(
                groupName = groupName,
                kafkaConnect = kafkaConnect,
                autoCreateTopicNames = if (autoCreateTopics) preparedTopics.keys else emptySet(),
                topics = preparedTopics,
                shouldValidate = enabled
            )
        }
    }
}


