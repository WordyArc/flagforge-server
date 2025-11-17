package dev.owlmajin.flagforge.server.common.kafka.topic

import dev.owlmajin.flagforge.server.common.kafka.DATA_SCHEMA_RUNTIME_V1_0_0
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.kafka.autoconfigure.KafkaProperties

private const val DELETE_POLICY = "delete"
private const val COMPACT_POLICY = "compact"

@ConfigurationProperties("app.persistence")
class PersistenceProperties {
    var enabled: Boolean = true
    var autoCreateTopics: Boolean = true
    var kafka: KafkaProperties = KafkaProperties()

    var commandMessages = TopicProperties()
        set(value) {
            field = value.overrideWith(field)
        }
    var eventMessages = TopicProperties()
        set(value) {
            field = value.overrideWith(field)
        }
    var flagState = TopicProperties()
        set(value) {
            field = value.overrideWith(field)
        }
    var segmentState = TopicProperties()
        set(value) {
            field = value.overrideWith(field)
        }
    var envState = TopicProperties()
        set(value) {
            field = value.overrideWith(field)
        }
    var projectState = TopicProperties()
        set(value) {
            field = value.overrideWith(field)
        }
    var sdkKeys = TopicProperties()
        set(value) {
            field = value.overrideWith(field)
        }

    var flagKeyIndex = TopicProperties()
        set(value) {
            field = value.overrideWith(field)
        }

    init {
        commandMessages = commandMessages.withDefaults(
            "command-messages",
            "CommandMessagesTopic",
            DELETE_POLICY,
            DATA_SCHEMA_RUNTIME_V1_0_0
        )
        eventMessages = eventMessages.withDefaults(
            "event-messages",
            "EventMessagesTopic",
            DELETE_POLICY,
            DATA_SCHEMA_RUNTIME_V1_0_0
        )
        flagState = flagState.withDefaults(
            "flag-state",
            "flagStateTopic",
            COMPACT_POLICY,
            DATA_SCHEMA_RUNTIME_V1_0_0
        )
        segmentState = segmentState.withDefaults(
            "segment-state",
            "segmentStateTopic",
            COMPACT_POLICY,
            DATA_SCHEMA_RUNTIME_V1_0_0
        )
        envState = envState.withDefaults(
            "env-state",
            "envStateTopic",
            COMPACT_POLICY,
            DATA_SCHEMA_RUNTIME_V1_0_0
        )
        projectState = projectState.withDefaults(
            "project-state",
            "projectStateTopic",
            COMPACT_POLICY,
            DATA_SCHEMA_RUNTIME_V1_0_0
        )
        sdkKeys = sdkKeys.withDefaults(
            "sdk-keys",
            "sdkKeysTopic",
            COMPACT_POLICY,
            DATA_SCHEMA_RUNTIME_V1_0_0
        )
        flagKeyIndex = flagKeyIndex.withDefaults(
            "flag-key-index",
            "flagKeyIndexTopic",
            COMPACT_POLICY,
            DATA_SCHEMA_RUNTIME_V1_0_0
        )
    }
}
