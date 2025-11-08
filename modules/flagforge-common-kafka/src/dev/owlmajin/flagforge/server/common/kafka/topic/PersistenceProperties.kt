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

    var flagCommands = TopicProperties()
        set(value) {
            field = value.overrideWith(field)
        }
    var flagEvents = TopicProperties()
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

    init {
        flagCommands = flagCommands.withDefaults(
            "flag-commands",
            "flagCommandsTopic",
            DELETE_POLICY,
            DATA_SCHEMA_RUNTIME_V1_0_0
        )
        flagEvents = flagEvents.withDefaults(
            "flag-events",
            "flagEventsTopic",
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
    }
}
