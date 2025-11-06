package dev.owlmajin.flagforge.server.common.kafka

import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.config.ConfigResource
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("dev.owlmajin.flagforge.server.common.kafka.utils")

internal fun TopicProperties.toNewTopic(): NewTopic {
    applyMetadataDefaults()

    val newTopic = NewTopic(name, partitions, replicationFactor.toShort())

    if (topicConfig.isNotEmpty()) {
        newTopic.configs(topicConfig)
    }

    return newTopic
}


internal fun TopicGroupStarter.createTopicsIfNeeded() {
    if (!isAnyEnabled() || autoCreateTopicNames.isEmpty()) return

    log.info("Starting create $groupName topic(s)")

    try {
        val notCreatedNames = kafkaConnect.getNotExistingTopics(autoCreateTopicNames)
        if (notCreatedNames.isEmpty()) {
            log.info("All $groupName topic(s) already exist")
            return
        } else {
            log.info("Not found $groupName topic(s): $notCreatedNames on broker ${bootstrapServers()}. Try to create...")
        }

        val newTopics = notCreatedNames.map { name ->
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
        validateTopicsExist()
        validateCleanupPolicy()
    } catch (e: Exception) {
        throw RuntimeException(
            "Kafka $groupName topics error. Check configuration or broker: ${bootstrapServers()} availability",
            e
        )
    }

    log.info("$groupName topics validation is done.")
}


private fun TopicGroupStarter.validateTopicsExist() {
    log.info("Validating $groupName topics kit...")
    val notExistingTopics = kafkaConnect.getNotExistingTopics(topics.keys)
    check(notExistingTopics.isEmpty()) {
        "Found not existing/unavailable $groupName topic(s): $notExistingTopics on ${bootstrapServers()} broker(s). " +
                "Create them (enable autoCreateTopics) or check permissions."
    }
}

private fun TopicGroupStarter.validateCleanupPolicy() {
    log.info("Validating $groupName cleanup.policy ...")

    val invalid = getInvalidCleanupPolicyTopicNames()
    check(invalid.isEmpty()) {
        "$groupName topic(s) ${invalid.joinToString()} have incorrect cleanup.policy!"
    }
}

private fun TopicGroupStarter.getInvalidCleanupPolicyTopicNames(): List<String> {
    if (topics.isEmpty()) return emptyList()

    val resources = topics.keys.map { ConfigResource(ConfigResource.Type.TOPIC, it) }

    val configs = kafkaConnect.adminClient
        .describeConfigs(resources)
        .all()
        .get()

    return configs.entries.mapNotNull { (resource, config) ->
        val actual = config.get("cleanup.policy")?.value()
        val expected = topics[resource.name()]?.topicConfig?.get("cleanup.policy")

        if (expected != null && actual != expected) {
            resource.name()
        } else {
            null
        }
    }
}

