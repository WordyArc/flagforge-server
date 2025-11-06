package dev.owlmajin.flagforge.server.common.kafka

import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.config.ConfigResource
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("dev.owlmajin.flagforge.server.common.kafka.utils")

internal fun TopicProperties.toNewTopic(): NewTopic {
    applyDefaults()

    val newTopic = NewTopic(name, partitions, replicationFactor)
    if (topicConfig.isNotEmpty()) {
        newTopic.configs(topicConfig)
    }
    return newTopic
}

internal fun TopicGroupStarter.createTopicsIfNeeded() {
    if (!isAnyEnabled() || autoCreateTopicNames.isEmpty()) return

    log.info("Starting create $groupName topic(s)")

    try {
        val notCreatedTopics = kafkaConnect.getNotExistingTopics(topics.keys)
        notCreatedTopics.ifEmpty {
            log.info("All $groupName topic(s) already exist")
            return
        }
        log.info("Not found $groupName topic(s): $notCreatedTopics on broker ${bootstrapServers()}. Try to create...")

        val newTopics = notCreatedTopics.map { name ->
            val topic = topics[name]
                ?: error("Topic '$name' not found in group '$groupName'")
            topic.toNewTopic()
        }.toSet()

        kafkaConnect.createOrUpdateTopic(newTopics)
    } catch (e: Exception) {
        throw RuntimeException(
            "Kafka $groupName topics creation error. Check configuration or broker: ${bootstrapServers()} availability",
            e
        )
    }
}

internal fun TopicGroupStarter.validateTopicsIfNeeded() {
    if (!isAnyEnabled()) return

    log.info("Start $groupName validation...")

    try {
        assertTopicsPresent()
        assertCleanupPolicyMatches()
    } catch (e: Exception) {
        throw RuntimeException(
            "Kafka $groupName topics error. Check configuration or broker: ${bootstrapServers()} availability",
            e
        )
    }

    log.info("$groupName topics validation is done.")
}


private fun TopicGroupStarter.assertTopicsPresent() {
    val missing = kafkaConnect.getNotExistingTopics(topics.keys)
    check(missing.isEmpty()) {
        "Topics not found for group '$groupName': $missing on ${bootstrapServers()}. " +
                "Either enable autoCreateTopics or create them manually."
    }
}

private fun TopicGroupStarter.assertCleanupPolicyMatches() {
    if (topics.isEmpty()) return

    val resources = topics.keys.map { ConfigResource(ConfigResource.Type.TOPIC, it) }

    val configs = kafkaConnect.adminClient
        .describeConfigs(resources)
        .all()
        .get()

    val mismatched = configs.entries.mapNotNull { (resource, config) ->
        val actual = config.get("cleanup.policy")?.value()
        val expected = topics[resource.name()]?.topicConfig?.get("cleanup.policy")

        if (expected != null && actual != expected) resource.name() else null
    }

    check(mismatched.isEmpty()) {
        "cleanup.policy differs from expected for topics in group '$groupName': $mismatched"
    }
}

private fun TopicGroupStarter.buildNewTopic(topic: TopicProperties) =
    NewTopic(topic.name, topic.partitions, topic.replicationFactor).configs(topic.topicConfig)

