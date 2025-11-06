package dev.owlmajin.flagforge.server.common.kafka

class TopicGroupStarter(
    val groupName: String,
    val kafkaConnect: KafkaConnect,
    val topics: Map<String, TopicProperties>,
    val autoCreateTopicNames: Set<String>,
    val shouldValidate: Boolean,
) {

    init {
        topics.values.forEach { it.applyDefaults() }
        if (autoCreateTopicNames.isNotEmpty()) { createTopicsIfNeeded() }
        if (shouldValidate) { validateTopicsIfNeeded() }
    }

    fun isAnyEnabled(): Boolean = topics.isNotEmpty()

    internal fun bootstrapServers() =
        kafkaConnect.kafkaAdmin.configurationProperties["bootstrap.servers"]?.toString() ?: "<unknown>"

    companion object {
        fun ofTopics(
            groupName: String,
            kafkaConnect: KafkaConnect,
            topics: Set<TopicProperties>,
            isAutoCreateEnabled: Boolean,
            shouldValidate: Boolean
        ): TopicGroupStarter {
            val preparedTopics = topics.associateBy { it.name }
            val autoCreateTopicNames = if (isAutoCreateEnabled) preparedTopics.keys else emptySet()
            return TopicGroupStarter(
                groupName = groupName,
                kafkaConnect = kafkaConnect,
                topics = preparedTopics,
                autoCreateTopicNames = autoCreateTopicNames,
                shouldValidate = shouldValidate
            )
        }
    }
}


