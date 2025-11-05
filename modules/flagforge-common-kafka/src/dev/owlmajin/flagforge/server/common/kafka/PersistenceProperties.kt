package dev.owlmajin.flagforge.server.common.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.kafka.autoconfigure.KafkaProperties

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
        flagCommands = flagCommands.withMetadata(
            "flag-commands",
            "flagCommandsTopic",
            DELETE_POLICY,
            "runtime"
        )
        flagEvents = flagEvents.withMetadata(
            "flag-events",
            "flagEventsTopic",
            DELETE_POLICY,
            "runtime"
        )
        flagState = flagState.withMetadata(
            "flag-state",
            "flagStateTopic",
            COMPACT_POLICY,
            "runtime"
        )
        segmentState = segmentState.withMetadata(
            "segment-state",
            "segmentStateTopic",
            COMPACT_POLICY,
            "runtime"
        )
        envState = envState.withMetadata(
            "env-state",
            "envStateTopic",
            COMPACT_POLICY,
            "runtime"
        )
        projectState = projectState.withMetadata(
            "project-state",
            "projectStateTopic",
            COMPACT_POLICY,
            "runtime"
        )
        sdkKeys = sdkKeys.withMetadata(
            "sdk-keys",
            "sdkKeysTopic",
            COMPACT_POLICY,
            "runtime"
        )
    }
}