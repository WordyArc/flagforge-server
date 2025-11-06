package dev.owlmajin.flagforge.server.common.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.kafka.autoconfigure.KafkaProperties

const val DATA_SCHEMA_RUNTIME_DATA: String = "https://flagforge.dev/event/runtime/1.0.0"
const val DATA_SCHEMA_HISTORIC_DATA: String = "https://flagforge.dev/event/historic/1.0.0"

private const val DELETE_POLICY = "delete"
private const val COMPACT_POLICY = "compact"

@ConfigurationProperties("app.persistence")
class PersistenceProperties {
    val enabled: Boolean = true
    val autoCreateTopics: Boolean = true
    val kafka: KafkaProperties = KafkaProperties()

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
            DATA_SCHEMA_RUNTIME_DATA
        )
        flagEvents = flagEvents.withDefaults(
            "flag-events",
            "flagEventsTopic",
            DELETE_POLICY,
            DATA_SCHEMA_RUNTIME_DATA
        )
        flagState = flagState.withDefaults(
            "flag-state",
            "flagStateTopic",
            COMPACT_POLICY,
            DATA_SCHEMA_RUNTIME_DATA
        )
        segmentState = segmentState.withDefaults(
            "segment-state",
            "segmentStateTopic",
            COMPACT_POLICY,
            DATA_SCHEMA_RUNTIME_DATA
        )
        envState = envState.withDefaults(
            "env-state",
            "envStateTopic",
            COMPACT_POLICY,
            DATA_SCHEMA_RUNTIME_DATA
        )
        projectState = projectState.withDefaults(
            "project-state",
            "projectStateTopic",
            COMPACT_POLICY,
            DATA_SCHEMA_RUNTIME_DATA
        )
        sdkKeys = sdkKeys.withDefaults(
            "sdk-keys",
            "sdkKeysTopic",
            COMPACT_POLICY,
            DATA_SCHEMA_RUNTIME_DATA
        )
    }
}